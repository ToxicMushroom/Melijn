package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import java.awt.Color

class TempMuteCommand : AbstractCommand("command.tempmute") {

    init {
        id = 27
        name = "tempmute"
        aliases = arrayOf("tm")
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context, syntax)
            return
        }
        val targetUser = getUserByArgsNMessage(context, 0) ?: return
        val member = context.getGuild().getMember(targetUser)
        if (member != null && !context.getGuild().selfMember.canInteract(member)) {
            val msg = Translateable("$root.cannotmute").string(context)
                    .replace("%user%", targetUser.asTag)
            sendMsg(context, msg)
            return
        }

        val noUserArg = context.rawArg.replaceFirst((context.args[0] + "($:\\s+)?").toRegex(), "")
        val noReasonArgs = noUserArg.split(">")[0].split("\\s+".toRegex())
        val muteDuration = (getDurationByArgsNMessage(context, noReasonArgs, 1, noReasonArgs.size) ?: return) * 1000

        var reason = if (noUserArg.contains(">"))
            noUserArg.substring(noUserArg.indexOfFirst { s -> s == '>' } + 1, noUserArg.length)
        else
            "/"

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
                                muteRoleAquired(context, targetUser, reason, role, muteDuration)
                            },
                            { failed ->
                                val msgFailed = Translateable("$root.failed.creatingmuterole").string(context)
                                        .replace("%cause%", failed.message ?: "unknown (contact support for info)")
                                sendMsg(context, msgFailed)
                            }
                    )
            return
        } else {
            muteRoleAquired(context, targetUser, reason, muteRole, muteDuration)
        }


    }

    private fun muteRoleAquired(context: CommandContext, targetUser: User, reason: String, muteRole: Role, muteDuration: Long) {
        val activeMute: Mute? = context.daoManager.muteWrapper.getActiveMute(context.getGuildId(), targetUser.idLong)
        val mute = Mute(
                context.getGuildId(),
                targetUser.idLong,
                context.authorId,
                reason,
                null,
                endTime = System.currentTimeMillis() + muteDuration
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
        val mutedMessage = getMuteMessage(context.getGuild(), targetUser, context.getAuthor(), mute)
        context.daoManager.muteWrapper.setMute(mute)
        val targetMember = context.getGuild().getMember(targetUser)
        if (targetMember == null) {

        } else {
            context.getGuild().addRoleToMember(targetMember, muteRole).queue({
                mutingMessage?.editMessage(
                        mutedMessage
                )?.override(true)?.queue()

                val logChannelWrapper = context.daoManager.logChannelWrapper
                val logChannelId = logChannelWrapper.logChannelCache.get(Pair(context.getGuildId(), LogChannelType.TEMP_MUTE)).get()
                val logChannel = context.getGuild().getTextChannelById(logChannelId)
                logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, mutedMessage) }

                val msg = Translateable("$root.success" + if (activeMute != null) ".updated" else "").string(context)
                        .replace("%user%", targetUser.asTag)
                        .replace("%endTime%", mute.endTime?.asEpochMillisToDateTime() ?: "none")
                        .replace("%reason%", mute.reason)
                sendMsg(context, msg)
            }, {
                mutingMessage?.editMessage("failed to mute")?.queue()
                val msg = Translateable("$root.failure").string(context)
                        .replace("%user%", targetUser.asTag)
                        .replace("%cause%", it.message ?: "unknown (contact support for info)")
                sendMsg(context, msg)
            })
        }
    }
}

fun getMuteMessage(guild: Guild, mutedUser: User, muteAuthor: User, mute: Mute): MessageEmbed {
    val banDuration = mute.endTime?.let { endTime ->
        getDurationString((endTime - mute.startTime))
    } ?: "infinite"

    val eb = EmbedBuilder()
    eb.setAuthor("Muted by: " + muteAuthor.asTag + " ".repeat(45).substring(0, 45 - muteAuthor.name.length) + "\u200B", null, muteAuthor.effectiveAvatarUrl)
    eb.setDescription("```LDIF" +
            "\nGuild: " + guild.name +
            "\nGuildId: " + guild.id +
            "\nMute Author: " + (muteAuthor.asTag) +
            "\nMute Author Id: " + mute.muteAuthorId +
            "\nMuted: " + mutedUser.asTag +
            "\nMutedId: " + mutedUser.id +
            "\nReason: " + mute.reason +
            "\nDuration: " + banDuration +
            "\nStart of mute: " + (mute.startTime.asEpochMillisToDateTime()) +
            "\nEnd of mute: " + (mute.endTime?.asEpochMillisToDateTime() ?: "none") + "```")
    eb.setThumbnail(mutedUser.effectiveAvatarUrl)
    eb.setColor(Color.BLUE)
    return eb.build()
}