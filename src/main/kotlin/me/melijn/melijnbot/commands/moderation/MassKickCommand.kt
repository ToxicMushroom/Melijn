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
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import java.awt.Color

class MassKickCommand : AbstractCommand("command.masskick") {

    init {
        name = "massKick"
        aliases = arrayOf("mk")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.KICK_MEMBERS)
    }

    companion object {
        fun getDefaultMessage(): ModularMessage {
            val embed = EmbedEditor().apply {
                setAuthor("{punishAuthorTag}{titleSpaces:{punishAuthorTag}}", null, "{punishAuthorAvatarUrl}")
                setColor(Color(0xDA31FC))
                setDescription("```LDIF\n")
                appendDescription(
                    """
            Kick Author: {punishAuthorTag}
            Kick Author Id: {punishAuthorId}
            KickedList: {punishList}
            Reason: {reason}
            Duration: {timeDuration}
            Moment of kick: {startTime:null}
            Case Id: {punishId}{if:{extraLcInfo}|=|null|then:|else:{extraLcInfo}}
        """.trimIndent()
                )
                appendDescription("```")
            }.build()
            return ModularMessage(null, embed)
        }
    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 1)) return

        var offset = 0
        val size = context.args.size
        val users = mutableListOf<Member>()
        for (i in 0 until size) {
            if (context.args[i] == "-r") {
                break
            }
            offset++
            val user = retrieveMemberByArgsNMessage(context, i, true) ?: return
            if (ModUtil.cantPunishAndReply(context, user)) return

            users.add(i, user)
        }

        var reason = context.getRawArgPart(1 + offset)

        if (reason.isBlank()) reason = "/"
        reason = reason.trim()

        var success = 0
        var failed = 0
        val daoManager = context.daoManager

        for (targetMember in users) {

            val kick = Kick(
                context.guildId,
                targetMember.idLong,
                context.authorId,
                reason
            )

            if (users.size < 11) {
                val kicking = context.getTranslation("message.kicking")
                targetMember.user.openPrivateChannel().awaitOrNull()?.let { pc ->
                    sendMsgAwaitEL(pc, kicking)
                }
            }
            if (continueKicking(context, targetMember, kick)) success++
            else failed++
        }

        val kick = Kick(
            context.guildId,
            users[0].idLong,
            context.authorId,
            reason
        )

        val warnedMessagesLc = getMassPunishMessages(
            daoManager,
            context.guild,
            users.map { it.user }.toSet(),
            context.author,
            kick,
            MessageType.WARN_LOG
        )

        context.guild.getAndVerifyLogChannelByType(context.daoManager, LogChannelType.MASS_KICK)?.let { tc ->
            warnedMessagesLc.forEach { msg ->
                sendMsg(tc, context.webManager.proxiedHttpClient, msg)
            }
        }

        val msg = context.getTranslation("$root.kicked.${if (failed == 0) "success" else "ok"}")
            .withVariable("success", success)
            .withVariable("failed", failed)
            .withSafeVarInCodeblock("reason", reason)

        sendRsp(context, msg)
    }

    private suspend fun continueKicking(
        context: ICommandContext,
        targetMember: Member,
        kick: Kick,
        kickingMessage: Message? = null
    ): Boolean {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager

        daoManager.kickWrapper.addKick(kick)

        val kickException = guild.kick(targetMember)
            .reason("(massKick) " + author.asTag + ": " + kick.reason)
            .awaitEX()
        val kickSuccess = kickException == null

        if (kickSuccess) {
            val kickedMessageDm = getPunishMessage(language, daoManager, guild, targetMember.user, author, kick, msgType = MessageType.MASS_KICK)
            kickingMessage?.editMessage(kickedMessageDm)?.override(true)?.queue()
        } else {
            val failedMsg = context.getTranslation("message.kicking.failed")
            kickingMessage?.editMessage(failedMsg)?.queue()
        }
        return kickSuccess
    }
}