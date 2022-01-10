package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.commandutil.moderation.ModUtil
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.models.EmbedEditor
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import java.awt.Color

class BanCommand : AbstractCommand("command.ban") {

    init {
        id = 24
        name = "ban"
        aliases = arrayOf("permBan", "hackBan")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    companion object {
        /** Helper function to create a ban object based on active ban, or just create a new one.
         *  @returns created ban object AND boolean == based on the active ban?
         **/
        suspend fun createBanFromActiveOrNew(
            context: ICommandContext,
            banned: User,
            reason: String
        ): Pair<Ban, Boolean> {
            val activeBan: Ban? = context.daoManager.banWrapper.getActiveBan(context.guildId, banned.idLong)
            val ban = Ban(context.guildId, banned.idLong, context.authorId, reason, null)
            if (activeBan != null) {
                ban.banId = activeBan.banId
                ban.startTime = activeBan.startTime
            }
            return ban to (activeBan != null)
        }

        fun getDefaultMessage(logChannel: Boolean): ModularMessage {
            val embed = EmbedEditor().apply {
                setAuthor("{punishAuthorTag}{titleSpaces:{punishAuthorTag}}", null, "{punishAuthorAvatarUrl}")
                setColor(Color.RED)
                setThumbnail("{punishedUserAvatarUrl}")
                setDescription("```LDIF\n")
                if (!logChannel) appendDescription("Server: {serverName}\nServer Id: {serverId}\n")
                appendDescription(
                    """
            Ban Author: {punishAuthorTag}
            Ban Author Id: {punishAuthorId}
            Banned: {punishedUserTag}
            Banned Id: {punishedUserId}
            Reason: {reason}
            Duration: {timeDuration}
            Start of ban: {startTime:${if (logChannel) "null" else "{punishedUserId}"}}
            End of ban: {endTime:${if (logChannel) "null" else "{punishedUserId}"}}
            Case Id: {punishId}{if:{extraLcInfo}|=|null|then:|else:{extraLcInfo}}
        """.trimIndent()
                )
                appendDescription("```")
            }.build()
            return ModularMessage(null, embed)
        }

        val optionalDeldaysPattern = "-t([0-7])".toRegex()
    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 0)) return

        // ban <user> -t1 reason
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull()
        if (member != null) if (ModUtil.cantPunishAndReply(context, member)) return

        // ban user <-t1> reason
        var deldays = 7
        var offset = 0
        if (context.args.size > 1) {
            val firstArg = context.args[1]
            if (firstArg.matches(optionalDeldaysPattern)) {
                offset = 1
                deldays = optionalDeldaysPattern.find(firstArg)?.groupValues?.get(1)?.toInt() ?: 0
            }
        }

        // ban user -t1 <reason>
        var reason = context.getRawArgPart(1 + offset)

        if (reason.isBlank()) reason = "/"
        reason = reason.trim()

        val (ban, updated) = createBanFromActiveOrNew(context, targetUser, reason)

        val message: Message? = TempBanCommand.sendBanningDM(context, targetUser)
        continueBanning(context, targetUser, ban, updated, deldays, message)
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
        val bannedMessageDm = getPunishMessage(lang, daoManager, guild, targetUser, author, ban, msgType = MessageType.BAN)
        val bannedMessageLc = getPunishMessage(
            lang, daoManager, guild, targetUser, author, ban, true,
            banningMessage != null, MessageType.BAN_LOG
        )

        val msg = try {
            guild.ban(targetUser, delDays)
                .reason("(ban) ${context.author.asTag}: " + ban.reason)
                .async { daoManager.banWrapper.setBan(ban) }

            banningMessage?.editMessage(bannedMessageDm)?.override(true)?.queue()

            val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.PERMANENT_BAN)
            logChannel?.let { it1 -> sendMsg(it1, context.webManager.proxiedHttpClient, bannedMessageLc) }

            context.getTranslation("$root.success" + if (updated) ".updated" else "")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
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

fun MessageType.getDefaultMsg(): ModularMessage {
    return when (this) {
        MessageType.PRE_VERIFICATION_JOIN -> TODO()
        MessageType.PRE_VERIFICATION_LEAVE -> TODO()
        MessageType.JOIN -> TODO()
        MessageType.LEAVE -> TODO()
        MessageType.BANNED -> TODO()
        MessageType.KICKED -> TODO()
        MessageType.BIRTHDAY -> TODO()
        MessageType.BOOST -> TODO()
        MessageType.BAN -> BanCommand.getDefaultMessage(false)
        MessageType.TEMP_BAN -> BanCommand.getDefaultMessage(false)
        MessageType.MASS_BAN -> BanCommand.getDefaultMessage(false)
        MessageType.SOFT_BAN -> SoftBanCommand.getDefaultMessage(false)
        MessageType.UNBAN -> UnbanCommand.getDefaultMessage(false)
        MessageType.MUTE -> MuteCommand.getDefaultMessage(false)
        MessageType.TEMP_MUTE -> MuteCommand.getDefaultMessage(false)
        MessageType.UNMUTE -> UnmuteCommand.getDefaultMessage(false)
        MessageType.MASS_KICK -> KickCommand.getDefaultMessage(false)
        MessageType.KICK -> KickCommand.getDefaultMessage(false)
        MessageType.WARN -> WarnCommand.getDefaultMessage(false)
        MessageType.MASS_BAN_LOG -> MassBanCommand.getDefaultMessage()
        MessageType.BAN_LOG -> BanCommand.getDefaultMessage(true)
        MessageType.TEMP_BAN_LOG -> BanCommand.getDefaultMessage(true)
        MessageType.SOFT_BAN_LOG -> SoftBanCommand.getDefaultMessage(true)
        MessageType.UNBAN_LOG -> UnbanCommand.getDefaultMessage(true)
        MessageType.MUTE_LOG -> MuteCommand.getDefaultMessage(true)
        MessageType.TEMP_MUTE_LOG -> MuteCommand.getDefaultMessage(true)
        MessageType.UNMUTE_LOG -> UnmuteCommand.getDefaultMessage(true)
        MessageType.MASS_KICK_LOG -> MassKickCommand.getDefaultMessage()
        MessageType.KICK_LOG -> KickCommand.getDefaultMessage(true)
        MessageType.WARN_LOG -> WarnCommand.getDefaultMessage(true)
        else -> throw IllegalArgumentException("Default message for ${this} not implemented")
    }
}

fun getAtLeastNCodePointsAfterName(user: User): String {
    val amount = 0.coerceAtLeast((45L - user.name.codePoints().count()).toInt())
    return " ".repeat(amount)
}