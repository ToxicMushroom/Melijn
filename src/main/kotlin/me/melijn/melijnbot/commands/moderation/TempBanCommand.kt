package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.commandutil.moderation.ModUtil
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.TimeFormat

class TempBanCommand : AbstractCommand("command.tempban") {

    init {
        id = 23
        name = "tempBan"
        aliases = arrayOf("tBan", "temporaryBan")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 1)) return

        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull()
        if (member != null) if (ModUtil.cantPunishAndReply(context, member)) return


        // ban user <-t1> reason
        var deldays = 7
        var offset = 0
        if (context.args.size > 2) {
            val firstArg = context.args[1]
            if (firstArg.matches(BanCommand.optionalDeldaysPattern)) {
                offset = 1
                deldays = BanCommand.optionalDeldaysPattern.find(firstArg)?.groupValues?.get(1)?.toInt() ?: 0
            }
        }

        val durationArgs = context.args[offset + 1].split(SPACE_PATTERN)
        val banDuration = (getDurationByArgsNMessage(context, 0, durationArgs.size, durationArgs) ?: return) * 1000

        var reason = context.getRawArgPart(offset + 2)
        if (reason.isBlank()) reason = "/"

        val activeBan = context.daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong)
        val ban = Ban(context.guildId, targetUser.idLong, context.authorId, reason, null,
            endTime = System.currentTimeMillis() + banDuration)

        if (activeBan != null) {
            ban.banId = activeBan.banId
            ban.startTime = activeBan.startTime
        }

        val message: Message? = sendBanningDM(context, targetUser)

        continueBanning(context, targetUser, ban, activeBan != null, deldays, message)
    }

    companion object {
        suspend fun sendBanningDM(
            context: ICommandContext,
            targetUser: User
        ): Message? {
            val banning = context.getTranslation("message.banning")

            val privateChannel = if (context.guild.isMember(targetUser)) targetUser.openPrivateChannel().awaitOrNull()
            else null

            val message: Message? = privateChannel?.let {
                sendMsgAwaitEL(it, banning)
            }?.firstOrNull()
            return message
        }
    }

    private suspend fun continueBanning(
        context: ICommandContext,
        targetUser: User,
        ban: Ban,
        updated: Boolean,
        delDays: Int,
        banningMessage: Message? = null
    ) {
        val guild = context.guild
        val author = context.author
        val lang = context.getLanguage()
        val daoManager = context.daoManager
        val bannedMessageDm =
            getPunishMessage(lang, daoManager, guild, targetUser, author, ban, msgType = MessageType.TEMP_BAN)
        val bannedMessageLc = getPunishMessage(lang, daoManager, guild, targetUser, author, ban, true,
            banningMessage != null, MessageType.TEMP_BAN_LOG)

        val msg = try {
            guild.ban(targetUser, delDays)
                .reason("(tempBan) ${context.author.asTag}: " + ban.reason)
                .async { daoManager.banWrapper.setBan(ban) }

            banningMessage?.editMessage(bannedMessageDm)?.override(true)?.queue()

            val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.TEMP_BAN)
            logChannel?.let { it1 -> sendMsg(it1, context.webManager.proxiedHttpClient, bannedMessageLc) }

            context.getTranslation("$root.success" + if (updated) ".updated" else "")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withVariable("endTime", ban.endTime?.let { TimeFormat.DATE_TIME_LONG.format(it) } ?: "none")
                .withSafeVarInCodeblock("reason", ban.reason)

        } catch (t: Throwable) {

            val failedMsg = context.getTranslation("message.banning.failed")
            banningMessage?.editMessage(failedMsg)?.queue()

            context.getTranslation("$root.failure")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withSafeVarInCodeblock("cause", t.message ?: "/")
        }
        sendRsp(context, msg)
    }
}