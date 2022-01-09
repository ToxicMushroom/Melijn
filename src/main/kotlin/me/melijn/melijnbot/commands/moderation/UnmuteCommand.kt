package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.commandutil.moderation.ModUtil
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.models.EmbedEditor
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.User
import java.awt.Color

class UnmuteCommand : AbstractCommand("command.unmute") {

    init {
        id = 26
        name = "unmute"
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.MANAGE_ROLES)
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
            Unmute Author: {unPunishAuthorTag}
            Unmute Author Id: {unPunishAuthorId}
            UnMuted: {punishedUserTag}
            UnMuted Id: {punishedUserId}
            Reason: {reason}
            Unmute Reason: {unPunishReason}
            Duration: {timeDuration}
            Start of muted: {startTime:${if (logChannel) "null" else "{punishedUserId}"}}
            End of mute: {endTime:${if (logChannel) "null" else "{punishedUserId}"}}
            Case Id: {punishId}{if:{extraLcInfo}|=|null|then:|else:{extraLcInfo}}
        """.trimIndent()
                )
                appendDescription("```")
            }.build())
        }
    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 0)) return

        val guild = context.guild
        val daoManager = context.daoManager
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return

        var unmuteReason = context.rawArg
            .removeFirst(context.args[0])
            .trim()

        if (unmuteReason.isBlank()) {
            unmuteReason = "/"
        }

        val activeMute = daoManager.muteWrapper.getActiveMute(context.guildId, targetUser.idLong)
        val mute = activeMute ?: Mute(guild.idLong, targetUser.idLong, null, "/")

        mute.unmuteAuthorId = context.authorId
        mute.unmuteReason = unmuteReason
        mute.endTime = System.currentTimeMillis()
        mute.active = false

        val muteAuthor = mute.muteAuthorId?.let { context.shardManager.retrieveUserById(it).awaitOrNull() }
        val targetMember = guild.retrieveMember(targetUser).awaitOrNull()

        val muteRole = guild.getAndVerifyRoleByType(daoManager, RoleType.MUTE)
        if (muteRole == null) {
            val msg = context.getTranslation("$root.nomuterole")
                .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            sendRsp(context, msg)
            return
        }

        if (targetMember != null && targetMember.roles.contains(muteRole)) {
            if (ModUtil.cantPunishAndReply(context, targetMember)) return

            val exception = guild.removeRoleFromMember(targetMember, muteRole)
                .reason("(unmute) ${context.author.asTag}: " + unmuteReason)
                .awaitEX()

            if (exception == null) {
                sendUnmuteLogs(context, targetUser, muteAuthor, mute, unmuteReason)
            } else {
                val msg = context.getTranslation("$root.failure")
                    .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
                    .withSafeVarInCodeblock("cause", exception.message ?: "/")
                sendRsp(context, msg)
            }

        } else if (targetMember == null) {
            sendUnmuteLogs(context, targetUser, muteAuthor, mute, unmuteReason)

        } else if (!targetMember.roles.contains(muteRole)) {
            val msg = context.getTranslation("$root.notmuted")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)

            sendRsp(context, msg)
        }

        if (activeMute != null) {
            daoManager.muteWrapper.setMute(mute)
        }
    }

    private suspend fun sendUnmuteLogs(
        context: ICommandContext,
        targetUser: User,
        muteAuthor: User?,
        mute: Mute,
        unmuteReason: String
    ) {
        val guild = context.guild
        val daoManager = context.daoManager
        val language = context.getLanguage()
        daoManager.muteWrapper.setMute(mute)
        val unpunishAuthor = context.author

        // Normal success path
        val unmuteMsgDm = getUnTempPunishMessage(
            language, daoManager, guild, targetUser, muteAuthor,
            unpunishAuthor, mute, msgType = MessageType.UNMUTE
        )
        val privateChannel = if (context.guild.isMember(targetUser)) targetUser.openPrivateChannel().awaitOrNull()
        else null


        val success = try {
            privateChannel?.let {
                sendMsg(it, context.webManager.proxiedHttpClient, unmuteMsgDm)
                true
            } ?: false
        } catch (t: Throwable) {
            false
        }

        val lcMsg = getUnTempPunishMessage(
            language, daoManager, guild, targetUser, muteAuthor,
            unpunishAuthor, mute, true, success, MessageType.UNMUTE_LOG
        )

        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNMUTE)
        logChannel?.let { it1 -> sendMsg(it1, context.webManager.proxiedHttpClient, lcMsg) }

        val successMsg = context.getTranslation("$root.success")
            .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
            .withSafeVarInCodeblock("reason", unmuteReason)

        sendRsp(context, successMsg)
    }
}