package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
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
        discordChannelPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val guild = context.guild
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = guild.retrieveMember(targetUser).await()
        if (member != null && !guild.selfMember.canInteract(member)) {
            val msg = context.getTranslation("message.interact.member.hierarchyexception")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
            sendMsg(context, msg)
            return
        }

        val noUserArg = context
            .rawArg.removeFirst(context.args[0])
            .trim()
        val noReasonArgs = noUserArg.split(">")[0].trim().split("\\s+".toRegex())
        val banDuration = (getDurationByArgsNMessage(context, 0, noReasonArgs.size, noReasonArgs) ?: return) * 1000

        var reason = if (noUserArg.contains(">")) {
            noUserArg.substring(noUserArg.indexOfFirst { s -> s == '>' } + 1, noUserArg.length)
        } else {
            "/"
        }

        reason = reason.trim()

        val activeBan: Ban? = context.daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong)
        val ban = Ban(
            context.guildId,
            targetUser.idLong,
            context.authorId,
            reason,
            null,
            endTime = System.currentTimeMillis() + banDuration
        )
        if (activeBan != null) {
            ban.banId = activeBan.banId
            ban.startTime = activeBan.startTime
        }

        val banning = context.getTranslation("message.banning")

        val privateChannel = targetUser.openPrivateChannel().awaitOrNull()
        val message: Message? = privateChannel?.let {
            sendMsgEL(it, banning)
        }?.firstOrNull()

        continueBanning(context, targetUser, ban, activeBan, message)
    }

    private suspend fun continueBanning(context: CommandContext, targetUser: User, ban: Ban, activeBan: Ban?, banningMessage: Message?) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetUser.idLong)

        val bannedMessageDm = getBanMessage(language, privZoneId, guild, targetUser, author, ban)
        val bannedMessageLc = getBanMessage(language, zoneId, guild, targetUser, author, ban, true, targetUser.isBot, banningMessage != null)
        context.daoManager.banWrapper.setBan(ban)

        try {
            context.guild.ban(targetUser, 7).await()
            banningMessage?.editMessage(
                bannedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.TEMP_BAN)).await()
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(daoManager.embedDisabledWrapper, it1, bannedMessageLc) }


            val endTime = ban.endTime?.asEpochMillisToDateTime(zoneId)
            val msg = context.getTranslation("$root.success" + if (activeBan != null) ".updated" else "")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%endTime%", endTime ?: "none")
                .replace("%reason%", ban.reason)
            sendMsg(context, msg)
        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.banning.failed")
            banningMessage?.editMessage(failedMsg)?.queue()

            val msg = context.getTranslation("$root.failure")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%cause%", t.message ?: "/")
            sendMsg(context, msg)
        }
    }
}