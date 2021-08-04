package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.commandutil.moderation.ModUtil
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.ban.SoftBan
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.jagtag.PunishJagTagParserArgs
import me.melijn.melijnbot.internals.jagtag.PunishmentJagTagParser
import me.melijn.melijnbot.internals.models.EmbedEditor
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import java.awt.Color

class SoftBanCommand : AbstractCommand("command.softban") {

    init {
        id = 111
        name = "softBan"
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    companion object {
        fun getDefaultMessage(logChannel: Boolean): ModularMessage {
            return ModularMessage(null, EmbedEditor().apply {
                setAuthor("{punishAuthorTag}{titleSpaces:{punishAuthorTag}}", null, "{punishAuthorAvatarUrl}")
                setColor(Color.ORANGE)
                setThumbnail("{punishedUserAvatarUrl}")
                setDescription("```LDIF\n")
                if (!logChannel) appendDescription("Server: {serverName}\nServer Id: {serverId}\n")
                appendDescription(
                    """
            SoftBan Author: {punishAuthorTag}
            SoftBan Author Id: {punishAuthorId}
            SoftBanned: {punishedUserTag}
            SoftBanned Id: {punishedUserId}
            Reason: {reason}
            Moment: {startTime:${if (logChannel) "null" else "{punishedUserId}"}}
            Case Id: {punishId}{if:{extraLcInfo}|=|null|then:|else:{extraLcInfo}}
        """.trimIndent()
                )
                appendDescription("```")
            }.build())
        }
    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 0)) return
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull()
        if (member != null) if (ModUtil.cantPunishAndReply(context, member)) return

        val clearDays = getIntegerFromArgN(context, 1, 1, 7)

        var reason = context.rawArg
            .removeFirst(context.args[0])
            .trim()

        if (context.args.size > 1 && clearDays != null) {
            reason = reason
                .removeFirst(context.args[1])
                .trim()
        }

        if (reason.isBlank()) reason = "/"
        reason = reason.trim()

        val hasActiveSoftBan: Boolean =
            context.daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong) != null
        val ban = SoftBan(
            context.guildId,
            targetUser.idLong,
            context.authorId,
            reason
        )

        val softBanning = context.getTranslation("message.softbanning")

        val privateChannel = if (context.guild.isMember(targetUser)) {
            targetUser.openPrivateChannel().awaitOrNull()
        } else {
            null
        }
        val message: Message? = privateChannel?.let {
            sendMsgAwaitEL(it, softBanning)
        }?.firstOrNull()

        continueBanning(context, targetUser, ban, hasActiveSoftBan, clearDays ?: 7, message)
    }

    private suspend fun continueBanning(
        context: ICommandContext,
        targetUser: User,
        softBan: SoftBan,
        hasActiveBan: Boolean,
        clearDays: Int,
        softBanningMessage: Message? = null
    ) {
        val guild = context.guild
        val author = context.author
        val lang = context.getLanguage()
        val daoManager = context.daoManager
        val softBannedMessageDm = getSoftBanMessage(lang, daoManager, guild, targetUser, author, softBan)
        val softBannedMessageLc = getSoftBanMessage(lang, daoManager, guild, targetUser, author, softBan,
            true, softBanningMessage != null)

        daoManager.softBanWrapper.addSoftBan(softBan)

        val msg = try {
            guild.ban(targetUser, clearDays)
                .reason("(softBan) ${context.author.asTag}: ${softBan.reason}")
                .await()

            softBanningMessage?.editMessage(softBannedMessageDm)?.override(true)?.queue()

            val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.SOFT_BAN)
            logChannel?.let { it1 -> sendMsg(it1, context.webManager.proxiedHttpClient, softBannedMessageLc) }

            if (!hasActiveBan) {
                guild.unban(targetUser)
                    .reason("(softBan) ${context.author.asTag}: ${softBan.reason}").await()
            }

            context.getTranslation("$root.success")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withSafeVarInCodeblock("reason", softBan.reason)

        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.softbanning.failed")
            softBanningMessage?.editMessage(failedMsg)?.queue()

            context.getTranslation("$root.failure")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withSafeVarInCodeblock("cause", t.message ?: "/")
        }
        sendRsp(context, msg)
    }
}

suspend fun getSoftBanMessage(
    language: String,
    daoManager: DaoManager,
    guild: Guild,
    bannedUser: User,
    banAuthor: User?,
    ban: SoftBan,
    lc: Boolean = false,
    received: Boolean = true
): ModularMessage {
    val isBot = bannedUser.isBot
    val extraDesc = if (!received || isBot)
        i18n.getTranslation(language, "message.punishment.extra." + if (isBot) "bot" else "dm")
    else "null"

    val msgType = if (lc) MessageType.SOFT_BAN_LOG else MessageType.SOFT_BAN
    val banDm = daoManager.linkedMessageWrapper.getMessage(guild.idLong, msgType)?.let {
        daoManager.messageWrapper.getMessage(guild.idLong, it)
    } ?: SoftBanCommand.getDefaultMessage(lc)

    val zoneId = getZoneId(daoManager, guild.idLong, if (lc) bannedUser.idLong else null)
    val args = PunishJagTagParserArgs(
        banAuthor, bannedUser, null,daoManager,  ban.reason, null,
        ban.moment, null, ban.softBanId, extraDesc, zoneId, guild
    )

    val message = banDm.mapAllStringFieldsSafe {
        if (it != null) PunishmentJagTagParser.parseJagTag(args, it)
        else null
    }

    return message
}