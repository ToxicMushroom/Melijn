package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.commandutil.moderation.ModUtil
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.ban.TempPunishment
import me.melijn.melijnbot.database.warn.Warn
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
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import java.awt.Color

class WarnCommand : AbstractCommand("command.warn") {

    init {
        id = 32
        name = "warn"
        commandCategory = CommandCategory.MODERATION
    }

    companion object {
        fun getDefaultMessage(logChannel: Boolean): ModularMessage {
            val embed = EmbedEditor().apply {
                setAuthor("{punishAuthorTag}{titleSpaces:{punishAuthorTag}}", null, "{punishAuthorAvatarUrl}")
                setColor(Color.YELLOW)
                setThumbnail("{punishedUserAvatarUrl}")
                setDescription("```LDIF\n")
                if (!logChannel) appendDescription("Server: {serverName}\nServer Id: {serverId}\n")
                appendDescription(
                    """
            Warn Author: {punishAuthorTag}
            Warn Author Id: {punishAuthorId}
            Warned: {punishedUserTag}
            Warned Id: {punishedUserId}
            Reason: {reason}
            Moment of Warn: {moment:${if (logChannel) "null" else "{punishedUserId}"}}
            Case Id: {punishId}{if:{extraLcInfo}|=|null|then:|else:{extraLcInfo}}
        """.trimIndent()
                )
                appendDescription("```")
            }.build()
            return ModularMessage(null, embed)
        }
    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 0)) return

        val targetMember = retrieveMemberByArgsNMessage(context, 0, true, botAllowed = false) ?: return
        if (ModUtil.cantPunishAndReply(context, targetMember)) return

        var reason = context.rawArg
            .removeFirst(context.args[0])
            .trim()

        if (reason.isBlank()) {
            reason = "/"
        }

        val warn = Warn(
            context.guildId,
            targetMember.idLong,
            context.authorId,
            reason
        )

        val warning = context.getTranslation("message.warning")

        val privateChannel = targetMember.user.openPrivateChannel().awaitOrNull()
        val message: Message? = privateChannel?.let {
            sendMsgAwaitEL(it, warning)
        }?.firstOrNull()

        continueWarning(context, targetMember, warn, message)
    }

    private suspend fun continueWarning(
        context: ICommandContext,
        targetMember: Member,
        warn: Warn,
        warningMessage: Message? = null
    ) {
        val guild = context.guild
        val author = context.author
        val daoManager = context.daoManager

        val language = context.getLanguage()
        val warnedMessageDm =
            getPunishMessage(language, daoManager, guild, targetMember.user, author, warn, msgType = MessageType.WARN)
        val warnedMessageLc = getPunishMessage(
            language,
            daoManager,
            guild,
            targetMember.user,
            author,
            warn,
            true,
            warningMessage != null, msgType = MessageType.WARN_LOG
        )

        context.daoManager.warnWrapper.addWarn(warn)

        warningMessage?.editMessage(
            warnedMessageDm
        )?.override(true)?.queue()

        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.WARN)
        logChannel?.let { it1 -> sendMsg(it1, context.webManager.proxiedHttpClient, warnedMessageLc) }

        val msg = context.getTranslation("$root.success")
            .withSafeVariable(PLACEHOLDER_USER, targetMember.asTag)
            .withSafeVarInCodeblock("reason", warn.reason)
        sendRsp(context, msg)
    }
}

suspend fun getPunishMessage(
    language: String,
    daoManager: DaoManager,
    guild: Guild,
    warnedUser: User,
    warnAuthor: User,
    punishment: TempPunishment,
    lc: Boolean = false,
    received: Boolean = true,
    msgType: MessageType
): ModularMessage {
    val isBot = warnedUser.isBot
    val extraDesc = if (!received || isBot)
        i18n.getTranslation(language, "message.punishment.extra." + if (isBot) "bot" else "dm")
    else "null"

    val dm = daoManager.linkedMessageWrapper.getMessage(guild.idLong, msgType)?.let {
        daoManager.messageWrapper.getMessage(guild.idLong, it)
    } ?: msgType.getDefaultMsg()

    val zoneId = getZoneId(daoManager, guild.idLong, if (lc) warnedUser.idLong else null)
    val args = PunishJagTagParserArgs(
        warnAuthor, warnedUser, null, daoManager, punishment.reason, punishment.dePunishReason,
        punishment.startTime, punishment.endTime, punishment.punishId, extraDesc, zoneId, guild
    )

    val message = dm.mapAllStringFieldsSafe {
        if (it != null) PunishmentJagTagParser.parseJagTag(args, it)
        else null
    }

    return message
}
