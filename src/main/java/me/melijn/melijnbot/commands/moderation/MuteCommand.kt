package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import java.awt.Color

class MuteCommand : AbstractCommand("command.mute") {

    init {
        id = 28
        name = "mute"
        aliases = arrayOf("m")
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context, syntax)
            return
        }
        val targetUser = getUserByArgsNMessage(context, 0) ?: return
        val member = context.getGuild().getMember(targetUser)
        if (member != null && !context.getGuild().selfMember.canInteract(member)) {
            val msg = Translateable("$root.cannotmute").string(context)
                    .replace(PLACEHOLDER_USER, targetUser.asTag)
            sendMsg(context, msg)
            return
        }

        var reason = context.rawArg.replaceFirst((context.args[0] + "($:\\s+)?").toRegex(), "")
        if (reason.isBlank()) reason = "/"

        var reasonPreSpaceCount = 0
        for (c in reason) {
            if (c == ' ') reasonPreSpaceCount++
            else break
        }
        reason = reason.substring(reasonPreSpaceCount)

        val roleId = context.daoManager.roleWrapper.roleCache.get(Pair(context.getGuildId(), RoleType.MUTE)).get()
        val muteRole: Role? = context.getGuild().getRoleById(roleId)
        if (muteRole == null) {
            val msg = Translateable("$root.creatingmuterole").string(context)
            sendMsg(context, msg)

            context.getGuild().createRole()
                    .setName("muted")
                    .setMentionable(false)
                    .setHoisted(false)
                    .setPermissions(Permission.MESSAGE_READ)
                    .queue(
                            { role ->
                                muteRoleAquired(context, targetUser, reason, role)
                            },
                            { failed ->
                                val msgFailed = Translateable("$root.failed.creatingmuterole").string(context)
                                        .replace("%cause%", failed.message ?: "unknown (contact support for info)")
                                sendMsg(context, msgFailed)
                            }
                    )
            return
        } else {
            muteRoleAquired(context, targetUser, reason, muteRole)
        }


    }

    private fun muteRoleAquired(context: CommandContext, targetUser: User, reason: String, muteRole: Role) {
        val activeMute: Mute? = context.daoManager.muteWrapper.getActiveMute(context.getGuildId(), targetUser.idLong)
        val mute = Mute(
                context.getGuildId(),
                targetUser.idLong,
                context.authorId,
                reason,
                null,
                endTime = null
        )
        if (activeMute != null) mute.startTime = activeMute.startTime


        val muting = Translateable("message.muting").string(context)
        targetUser.openPrivateChannel().queue({ privateChannel ->
            privateChannel.sendMessage(muting).queue({ message ->
                continueMuting(context, muteRole, targetUser, mute, activeMute, message)
            }, {
                continueMuting(context, muteRole, targetUser, mute, activeMute)
            })
        }, {
            continueMuting(context, muteRole, targetUser, mute, activeMute)
        })
    }

    private fun continueMuting(context: CommandContext, muteRole: Role, targetUser: User, mute: Mute, activeMute: Mute?, mutingMessage: Message? = null) {
        val guild = context.getGuild()
        val author = context.getAuthor()
        val mutedMessageDm = getMuteMessage(guild, targetUser, author, mute)
        val mutedMessageLc = getMuteMessage(guild, targetUser, author, mute, true, targetUser.isBot, mutingMessage != null)

        context.daoManager.muteWrapper.setMute(mute)
        val targetMember = guild.getMember(targetUser) ?: return

        guild.addRoleToMember(targetMember, muteRole).queue({
            mutingMessage?.editMessage(
                    mutedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = context.daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.PERMANENT_MUTE)).get()
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, mutedMessageLc) }

            val msg = Translateable("$root.success" + if (activeMute != null) ".updated" else "").string(context)
                    .replace(PLACEHOLDER_USER, targetUser.asTag)
                    .replace("%reason%", mute.reason)
            sendMsg(context, msg)
        }, {
            mutingMessage?.editMessage("failed to mute")?.queue()
            val msg = Translateable("$root.failure").string(context)
                    .replace(PLACEHOLDER_USER, targetUser.asTag)
                    .replace("%cause%", it.message ?: "unknown (contact support for info)")
            sendMsg(context, msg)
        })
    }
}

fun getMuteMessage(
        guild: Guild,
        mutedUser: User,
        muteAuthor: User,
        mute: Mute,
        lc: Boolean = false,
        isBot: Boolean = false,
        received: Boolean = true
): MessageEmbed {
    val eb = EmbedBuilder()

    val muteDuration = mute.endTime?.let { endTime ->
        getDurationString((endTime - mute.startTime))
    } ?: "infinite"

    val description = "```LDIF" +
            if (!lc) {
                "" +
                        "\nGuild: " + guild.name +
                        "\nGuildId: " + guild.id
            } else {
                ""
            } +
            "\nMute Author: " + (muteAuthor.asTag) +
            "\nMute Author Id: " + mute.muteAuthorId +
            "\nMuted: " + mutedUser.asTag +
            "\nMutedId: " + mutedUser.id +
            "\nReason: " + mute.reason +
            "\nDuration: " + muteDuration +
            "\nStart of mute: " + (mute.startTime.asEpochMillisToDateTime()) +
            "\nEnd of mute: " + (mute.endTime?.asEpochMillisToDateTime() ?: "none") + "```"
    if (!received || isBot) {
        "\nExtra: " +
                if (isBot) {
                    "Target is a bot"
                } else {
                    "Target had dm's disabled"
                }
    } else {
        ""
    } + "```"

    eb.setAuthor("Muted by: " + muteAuthor.asTag + " ".repeat(45).substring(0, 45 - muteAuthor.name.length) + "\u200B", null, muteAuthor.effectiveAvatarUrl)
    eb.setDescription(description)
    eb.setThumbnail(mutedUser.effectiveAvatarUrl)
    eb.setColor(Color.BLUE)
    return eb.build()
}