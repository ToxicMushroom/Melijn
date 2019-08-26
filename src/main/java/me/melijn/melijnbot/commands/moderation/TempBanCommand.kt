package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User

class TempBanCommand : AbstractCommand("command.tempban") {

    init {
        id = 23
        name = "tempBan"
        aliases = arrayOf("temporaryBan")
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context, syntax)
            return
        }
        val targetUser = getUserByArgsNMessage(context, 0) ?: return
        val member = context.getGuild().getMember(targetUser)
        if (member != null) {
            if (!context.getGuild().selfMember.canInteract(member)) {
                val msg = Translateable("$root.cannotban").string(context)
                        .replace(PLACEHOLDER_USER, targetUser.asTag)
                sendMsg(context, msg)
                return
            }
        }

        val noUserArg = context.rawArg.replaceFirst((context.args[0] + "($:\\s+)?").toRegex(), "")
        val noReasonArgs = noUserArg.split(">")[0].split("\\s+".toRegex())
        val banDuration = (getDurationByArgsNMessage(context, noReasonArgs, 1, noReasonArgs.size) ?: return) * 1000

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

        val activeBan: Ban? = context.daoManager.banWrapper.getActiveBan(context.getGuildId(), targetUser.idLong)
        val ban = Ban(
                context.getGuildId(),
                targetUser.idLong,
                context.authorId,
                reason,
                null,
                endTime = System.currentTimeMillis() + banDuration
        )
        if (activeBan != null) ban.startTime = activeBan.startTime

        //open user channel -> send "banning.." to user -> ban -> log to logchannel, edit "banning.." message to banmessage, reply with ban success


        val banning = Translateable("message.banning").string(context)
        targetUser.openPrivateChannel().queue({ privateChannel ->
            privateChannel.sendMessage(banning).queue({ message ->
                continueBanning(context, targetUser, ban, activeBan, message)
            }, {
                continueBanning(context, targetUser, ban, activeBan)
            })
        }, {
            continueBanning(context, targetUser, ban, activeBan)
        })
    }

    private fun continueBanning(context: CommandContext, targetUser: User, ban: Ban, activeBan: Ban?, banningMessage: Message? = null) {
        val guild = context.getGuild()
        val author = context.getAuthor()
        val bannedMessageDm = getBanMessage(guild, targetUser, author, ban)
        val bannedMessageLc = getBanMessage(guild, targetUser, author, ban, true, targetUser.isBot, banningMessage != null)
        context.daoManager.banWrapper.setBan(ban)
        context.getGuild().ban(targetUser, 7).queue({
            banningMessage?.editMessage(
                    bannedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = context.daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.TEMP_BAN)).get()
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, bannedMessageLc) }

            val msg = Translateable("$root.success" + if (activeBan != null) ".updated" else "").string(context)
                    .replace(PLACEHOLDER_USER, targetUser.asTag)
                    .replace("%endTime%", ban.endTime?.asEpochMillisToDateTime() ?: "none")
                    .replace("%reason%", ban.reason)
            sendMsg(context, msg)
        }, {
            banningMessage?.editMessage("failed to ban")?.queue()
            val msg = Translateable("$root.failure").string(context)
                    .replace(PLACEHOLDER_USER, targetUser.asTag)
                    .replace("%cause%", it.message ?: "unknown (contact support for info)")
            sendMsg(context, msg)
        })
    }
}