package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.i18n
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

    override suspend fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context, syntax)
            return
        }

        val guild = context.getGuild()
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = guild.getMember(targetUser)
        if (member != null && !guild.selfMember.canInteract(member)) {
            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.cannotban")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
            sendMsg(context, msg)
            return
        }

        val noUserArg = context
            .rawArg.replaceFirst(context.args[0], "")
            .trim()
        val noReasonArgs = noUserArg.split(">")[0].split("\\s+".toRegex())
        val banDuration = (getDurationByArgsNMessage(context, noReasonArgs, 1, noReasonArgs.size) ?: return) * 1000

        var reason = if (noUserArg.contains(">")) {
            noUserArg.substring(noUserArg.indexOfFirst { s -> s == '>' } + 1, noUserArg.length)
        } else {
            "/"
        }

        var reasonPreSpaceCount = 0
        for (c in reason) {
            if (c == ' ') {
                reasonPreSpaceCount++
            } else {
                break
            }
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
        if (activeBan != null) {
            ban.startTime = activeBan.startTime
        }

        val language = context.getLanguage()
        val banning = i18n.getTranslation(language, "message.banning")
        try {
            val privateChannel = targetUser.openPrivateChannel().await()
            val message = privateChannel.sendMessage(banning).await()
            continueBanning(context, targetUser, ban, activeBan, message)
        } catch (t: Throwable) {
            continueBanning(context, targetUser, ban, activeBan)
        }
    }

    private suspend fun continueBanning(context: CommandContext, targetUser: User, ban: Ban, activeBan: Ban?, banningMessage: Message? = null) {
        val guild = context.getGuild()
        val author = context.getAuthor()
        val bannedMessageDm = getBanMessage(guild, targetUser, author, ban)
        val bannedMessageLc = getBanMessage(guild, targetUser, author, ban, true, targetUser.isBot, banningMessage != null)
        context.daoManager.banWrapper.setBan(ban)

        val language = context.getLanguage()
        try {
            context.getGuild().ban(targetUser, 7).await()
            banningMessage?.editMessage(
                bannedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = context.daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.TEMP_BAN)).await()
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, bannedMessageLc) }


            val msg = i18n.getTranslation(language, "$root.success" + if (activeBan != null) ".updated" else "")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%endTime%", ban.endTime?.asEpochMillisToDateTime() ?: "none")
                .replace("%reason%", ban.reason)
            sendMsg(context, msg)
        } catch (t: Throwable) {
            banningMessage?.editMessage("failed to ban")?.queue()

            val msg = i18n.getTranslation(language, "$root.failure")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%cause%", t.message ?: "unknown (contact support for info)")
            sendMsg(context, msg)
        }
    }
}