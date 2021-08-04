package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.ban.Ban
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
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitN
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import java.awt.Color

class UnbanCommand : AbstractCommand("command.unban") {

    init {
        id = 25
        name = "unban"
        aliases = arrayOf("pardon")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 0)) return

        val guild = context.guild
        val daoManager = context.daoManager
        val language = context.getLanguage()
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return

        var unbanReason = context.rawArg
            .removeFirst(context.args[0])
            .trim()
        if (unbanReason.isBlank()) unbanReason = "/"

        unbanReason = unbanReason.trim()

        val activeBan = daoManager.banWrapper.getActiveBan(context.guildId, targetUser.idLong)
        val ban = activeBan ?: Ban(context.guildId, targetUser.idLong, null, "/")
        ban.unbanAuthorId = context.authorId
        ban.unbanReason = unbanReason
        ban.endTime = System.currentTimeMillis()
        ban.active = false

        val banAuthor = ban.banAuthorId?.let { context.shardManager.retrieveUserById(it).awaitOrNull() }

        try {
            guild.retrieveBan(targetUser).await()
            try {
                guild
                    .unban(targetUser)
                    .reason("(unban) ${context.author.asTag}: " + unbanReason)
                    .await()

                daoManager.banWrapper.setBan(ban)

                val msgLc = getUnbanMessage(language, context.daoManager, context.guild, targetUser,
                    banAuthor, context.author, ban, true)

                val privateChannel =
                    if (context.guild.isMember(targetUser)) targetUser.openPrivateChannel().awaitOrNull()
                    else null

                privateChannel?.let {
                    try {
                        val msg = sendMsgAwaitN(it, context.webManager.proxiedHttpClient, msgLc)
                        continueUnbanning(context, targetUser, ban, banAuthor, msg)
                    } catch (t: Throwable) {
                        continueUnbanning(context, targetUser, ban, banAuthor, null)
                    }
                } ?: continueUnbanning(context, targetUser, ban, banAuthor, null)

            } catch (t: Throwable) {
                // Sum ting wrong
                val msg = context.getTranslation("$root.failure")
                    .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                    .withSafeVariable("cause", t.message ?: "/")
                sendRsp(context, msg)
            }
        } catch (t: Throwable) {
            // Not banned anymore

            val msg = context.getTranslation("$root.notbanned")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
            sendRsp(context, msg)

            if (activeBan != null) {
                context.daoManager.banWrapper.setBan(ban)
            }
        }
    }

    companion object {
        fun getDefaultMessage(logChannel: Boolean): ModularMessage {
            return ModularMessage(null, EmbedEditor().apply {
                setAuthor("{unPunishAuthorTag}{titleSpaces:{unPunishAuthorTag}}", null, "{unPunishAuthorAvatarUrl}")
                setColor(Color.GREEN)
                setThumbnail("{punishedUserAvatarUrl}")
                setDescription("```LDIF\n")
                if (!logChannel) appendDescription("Server: {serverName}\nServer Id: {serverId}\n")
                appendDescription(
                    """
            Unban Author: {unPunishAuthorTag}
            Unban Author Id: {unPunishAuthorId}
            Unbanned: {punishedUserTag}
            Unbanned Id: {punishedUserId}
            Reason: {reason}
            Duration: {timeDuration}
            Start of ban: {startTime:${if (logChannel) "null" else "{punishedUserId}"}}
            End of ban: {endTime:${if (logChannel) "null" else "{punishedUserId}"}}
            Case Id: {punishId}{if:{extraLcInfo}|=|null|then:|else:{extraLcInfo}}
        """.trimIndent()
                )
                appendDescription("```")
            }.build())
        }
    }
    private suspend fun continueUnbanning(
        context: ICommandContext,
        targetUser: User,
        ban: Ban,
        banAuthor: User?,
        unbanningMessage: Message? = null
    ) {
        val guild = context.guild
        val unbanAuthor = context.author
        val daoManager = context.daoManager
        val language = context.getLanguage()
        val received = unbanningMessage != null
        val lcMsg = getUnbanMessage(language, daoManager, guild, targetUser, banAuthor, unbanAuthor, ban,
            true, received)

        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNBAN)
        logChannel?.let { it1 -> sendMsg(it1, context.webManager.proxiedHttpClient, lcMsg) }

        val success = context.getTranslation("$root.success")
            .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
            .withSafeVarInCodeblock("reason", ban.unbanReason ?: "/")
        sendRsp(context, success)
    }
}

suspend fun getUnbanMessage(
    language: String,
    daoManager: DaoManager,
    guild: Guild,
    bannedUser: User,
    banAuthor: User?,
    unbanAuthor: User,
    ban: Ban,
    lc: Boolean = false,
    received: Boolean = true
): ModularMessage {
    val isBot = bannedUser.isBot
    val extraDesc = if (!received || isBot)
        i18n.getTranslation(language, "message.punishment.extra." + if (isBot) "bot" else "dm")
    else "null"

    val msgType = if (lc) MessageType.UNBAN_LOG else MessageType.UNBAN
    val banDm = daoManager.linkedMessageWrapper.getMessage(guild.idLong, msgType)?.let {
        daoManager.messageWrapper.getMessage(guild.idLong, it)
    } ?: UnbanCommand.getDefaultMessage(lc)

    val zoneId = getZoneId(daoManager, guild.idLong, if (lc) bannedUser.idLong else null)
    val args = PunishJagTagParserArgs(
        banAuthor, bannedUser, unbanAuthor,daoManager,  ban.reason, ban.unbanReason,
        ban.startTime, ban.endTime, ban.banId, extraDesc, zoneId, guild
    )

    val message = banDm.mapAllStringFieldsSafe {
        if (it != null) PunishmentJagTagParser.parseJagTag(args, it)
        else null
    }

    return message
}