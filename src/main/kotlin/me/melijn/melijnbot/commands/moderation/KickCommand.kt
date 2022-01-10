package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.commandutil.moderation.ModUtil
import me.melijn.melijnbot.database.kick.Kick
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.models.EmbedEditor
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import java.awt.Color

class KickCommand : AbstractCommand("command.kick") {

    init {
        id = 30
        name = "kick"
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.KICK_MEMBERS)
    }

    companion object{
        fun getDefaultMessage(logChannel: Boolean): ModularMessage {
            val embed = EmbedEditor().apply {
                setAuthor("{punishAuthorTag}{titleSpaces:{punishAuthorTag}}", null, "{punishAuthorAvatarUrl}")
                setColor(Color.ORANGE)
                setThumbnail("{punishedUserAvatarUrl}")
                setDescription("```LDIF\n")
                if (!logChannel) appendDescription("Server: {serverName}\nServer Id: {serverId}\n")
                appendDescription(
                    """
            Kick Author: {punishAuthorTag}
            Kick Author Id: {punishAuthorId}
            Kicked: {punishedUserTag}
            Kicked Id: {punishedUserId}
            Reason: {reason}
            Moment of kick: {moment:${if (logChannel) "null" else "{punishedUserId}"}}
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
        if (reason.isBlank()) reason = "/"
        reason = reason.trim()

        val kick = Kick(
            context.guildId,
            targetMember.idLong,
            context.authorId,
            reason
        )

        val kicking = context.getTranslation("message.kicking")
        val privateChannel = targetMember.user.openPrivateChannel().awaitOrNull()
        val message: Message? = privateChannel?.let {
            sendMsgAwaitEL(it, kicking)
        }?.firstOrNull()

        continueKicking(context, targetMember, kick, message)
    }

    private suspend fun continueKicking(
        context: ICommandContext,
        targetMember: Member,
        kick: Kick,
        kickingMessage: Message? = null
    ) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager

        val kickedMessageDm = getPunishMessage(language, daoManager, guild, targetMember.user, author, kick, msgType = MessageType.KICK)
        val warnedMessageLc = getPunishMessage(
            language,
            daoManager,
            guild,
            targetMember.user,
            author,
            kick,
            true,
            kickingMessage != null,
            MessageType.KICK_LOG
        )

        context.daoManager.kickWrapper.addKick(kick)
        val msg = try {
            context.guild
                .kick(targetMember)
                .reason("(kick) " + context.author.asTag + ": " + kick.reason)
                .await()

            kickingMessage?.editMessage(
                kickedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = context.daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.getChannelId(guild.idLong, LogChannelType.KICK)
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendMsg(it1, context.webManager.proxiedHttpClient, warnedMessageLc) }

            context.getTranslation("$root.success")
                .withSafeVariable(PLACEHOLDER_USER, targetMember.asTag)
                .withSafeVarInCodeblock("reason", kick.reason)

        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.kicking.failed")
            kickingMessage?.editMessage(failedMsg)?.queue()

            context.getTranslation("$root.failure")
                .withSafeVariable(PLACEHOLDER_USER, targetMember.asTag)
                .withSafeVarInCodeblock("cause", t.message ?: "/")

        }
        sendRsp(context, msg)
    }
}