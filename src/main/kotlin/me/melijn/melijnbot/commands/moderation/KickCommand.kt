package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.kick.Kick
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.translation.MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import java.awt.Color
import java.time.ZoneId

class KickCommand : AbstractCommand("command.kick") {

    init {
        id = 30
        name = "kick"
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.KICK_MEMBERS)
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val targetMember = retrieveMemberByArgsNMessage(context, 0, true, botAllowed = false) ?: return
        if (!context.guild.selfMember.canInteract(targetMember)) {
            val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                .withVariable(PLACEHOLDER_USER, targetMember.asTag)
            sendRsp(context, msg)
            return
        }

        if (!context.member.canInteract(targetMember) && !hasPermission(
                context,
                SpecialPermission.PUNISH_BYPASS_HIGHER.node,
                true
            )
        ) {
            val msg = context.getTranslation(MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION)
                .withVariable(PLACEHOLDER_USER, targetMember.asTag)
            sendRsp(context, msg)
            return
        }

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
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetMember.idLong)

        val kickedMessageDm = getKickMessage(language, privZoneId, guild, targetMember.user, author, kick)
        val warnedMessageLc = getKickMessage(
            language,
            zoneId,
            guild,
            targetMember.user,
            author,
            kick,
            true,
            targetMember.user.isBot,
            kickingMessage != null
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
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, warnedMessageLc) }

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

fun getKickMessage(
    language: String,
    zoneId: ZoneId,
    guild: Guild,
    kickedUser: User,
    kickAuthor: User,
    kick: Kick,
    lc: Boolean = false,
    isBot: Boolean = false,
    received: Boolean = true
): MessageEmbed {
    var description = "```LDIF\n"
    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.description.nlc")
            .withSafeVarInCodeblock("serverName", guild.name)
            .withVariable("serverId", guild.id)
    }

    description += i18n.getTranslation(language, "message.punishment.kick.description")
        .withSafeVarInCodeblock("kickAuthor", kickAuthor.asTag)
        .withVariable("kickAuthorId", kickAuthor.id)
        .withSafeVarInCodeblock("kicked", kickedUser.asTag)
        .withVariable("kickedId", kickedUser.id)
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

    return EmbedBuilder()
        .setAuthor(author, null, kickAuthor.effectiveAvatarUrl)
        .setDescription(description)
        .setThumbnail(kickedUser.effectiveAvatarUrl)
        .setColor(Color.ORANGE)
        .build()
}