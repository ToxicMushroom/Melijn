package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.commandutil.moderation.ModUtil
import me.melijn.melijnbot.database.kick.Kick
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import java.awt.Color
import java.time.ZoneId

class MassKickCommand : AbstractCommand("command.masskick") {

    init {
        name = "massKick"
        aliases = arrayOf("mk")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.KICK_MEMBERS)
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

        val warnedMessagesLc = getMassKickMessage(
            context.getLanguage(),
            context.getTimeZoneId(),
            context.guild,
            users,
            context.author,
            kick,
        )

        context.guild.getAndVerifyLogChannelByType(context.daoManager, LogChannelType.MASS_KICK)?.let { tc ->
            warnedMessagesLc.forEach { msg ->
                sendEmbed(context.daoManager.embedDisabledWrapper, tc, msg)
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
        val privZoneId = getZoneId(daoManager, guild.idLong, targetMember.idLong)

        daoManager.kickWrapper.addKick(kick)

        val kickException = guild.kick(targetMember)
            .reason("(massKick) " + author.asTag + ": " + kick.reason)
            .awaitEX()
        val kickSuccess = kickException == null

        if (kickSuccess) {
            val kickedMessageDm = getKickMessage(language, privZoneId, guild, targetMember.user, author, kick)
            kickingMessage?.editMessage(kickedMessageDm)?.override(true)?.queue()
        } else {
            val failedMsg = context.getTranslation("message.kicking.failed")
            kickingMessage?.editMessage(failedMsg)?.queue()
        }
        return kickSuccess
    }

    fun getMassKickMessage(
        language: String,
        zoneId: ZoneId,
        guild: Guild,
        kickedUsers: List<Member>,
        kickAuthor: User,
        kick: Kick,
        lc: Boolean = false,
        isBot: Boolean = false,
        received: Boolean = true
    ): List<MessageEmbed> {
        var description = "```LDIF\n"
        if (!lc) {
            description += i18n.getTranslation(language, "message.punishment.description.nlc")
                .withSafeVarInCodeblock("serverName", guild.name)
                .withVariable("serverId", guild.id)
        }
        val users = mutableListOf<User>()
        for ((i, member) in kickedUsers.withIndex()) {
            users.add(i, member.user)
        }
        val bannedList = users.joinToString(separator = "\n- ", prefix = "\n- ") { "${it.id} - [${it.asTag}]" }

        description += i18n.getTranslation(language, "message.punishment.masskick.description")
            .withSafeVarInCodeblock("kickAuthor", kickAuthor.asTag)
            .withVariable("kickAuthorId", kickAuthor.id)
            .withSafeVarInCodeblock("kickedList", bannedList)
            .withSafeVarInCodeblock("reason", kick.reason)
            .withVariable("moment", (kick.moment.asEpochMillisToDateTime(zoneId)))
            .withVariable("kickId", kick.kickId)

        val extraDesc: String = if (!received || isBot) {
            i18n.getTranslation(
                language,
                if (isBot) {
                    "message.punishment.extra.bot"
                } else {
                    "message.punishment.extra.dm"
                }
            )
        } else {
            ""
        }
        description += extraDesc
        description += "```"

        val author = i18n.getTranslation(language, "message.punishment.kick.author")
            .withSafeVariable(PLACEHOLDER_USER, kickAuthor.asTag)
            .withVariable("spaces", getAtLeastNCodePointsAfterName(kickAuthor) + "\u200B")

        val list = StringUtils.splitMessageWithCodeBlocks(description, lang = "LDIF")
            .withIndex()
            .map { (index, part) ->
                val eb = EmbedBuilder()
                    .setDescription(part)
                    .setColor(Color.ORANGE)
                if (index == 0) eb.setAuthor(author, null, kickAuthor.effectiveAvatarUrl)
                eb.build()
            }

        return list
    }
}