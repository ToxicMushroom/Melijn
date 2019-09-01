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
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User

class TempMuteCommand : AbstractCommand("command.tempmute") {

    init {
        id = 27
        name = "tempmute"
        aliases = arrayOf("tm")
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
                                muteRoleAcquired(context, targetUser, reason, role, muteDuration)
                            },
                            { failed ->
                                val msgFailed = Translateable("$root.failed.creatingmuterole").string(context)
                                        .replace("%cause%", failed.message ?: "unknown (contact support for info)")
                                sendMsg(context, msgFailed)
                            }
                    )
            return
        } else {
            muteRoleAcquired(context, targetUser, reason, muteRole, muteDuration)
        }


    }

    private fun muteRoleAcquired(context: CommandContext, targetUser: User, reason: String, muteRole: Role, muteDuration: Long) {
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
        val guild = context.getGuild()
        val author = context.getAuthor()
        val mutedMessageDm = getMuteMessage(guild, targetUser, author, mute)
        val mutedMessageLc = getMuteMessage(guild, targetUser, author, mute, true, targetUser.isBot, mutingMessage != null)
        context.daoManager.muteWrapper.setMute(mute)
        val targetMember = guild.getMember(targetUser)
        if (targetMember != null) {
            context.getGuild().addRoleToMember(targetMember, muteRole).queue({
                death(mutingMessage, mutedMessageDm, context, mutedMessageLc, activeMute, mute, targetUser)
            }, {
                mutingMessage?.editMessage("failed to mute")?.queue()
                val msg = Translateable("$root.failure").string(context)
                        .replace(PLACEHOLDER_USER, targetUser.asTag)
                        .replace("%cause%", it.message ?: "unknown (contact support for info)")
                sendMsg(context, msg)
            })
        } else {
            death(mutingMessage, mutedMessageDm, context, mutedMessageLc, activeMute, mute, targetUser)
        }
    }

    private fun death(mutingMessage: Message?, mutedMessageDm: MessageEmbed, context: CommandContext, mutedMessageLc: MessageEmbed, activeMute: Mute?, mute: Mute, targetUser: User) {
        mutingMessage?.editMessage(
                mutedMessageDm
        )?.override(true)?.queue()

        val logChannelWrapper = context.daoManager.logChannelWrapper
        val logChannelId = logChannelWrapper.logChannelCache.get(Pair(context.getGuildId(), LogChannelType.TEMP_MUTE)).get()
        val logChannel = context.getGuild().getTextChannelById(logChannelId)
        logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, mutedMessageLc) }

        val msg = Translateable("$root.success" + if (activeMute != null) ".updated" else "").string(context)
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%endTime%", mute.endTime?.asEpochMillisToDateTime() ?: "none")
                .replace("%reason%", mute.reason)
        sendMsg(context, msg)
    }
}

