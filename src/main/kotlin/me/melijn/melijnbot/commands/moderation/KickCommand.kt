package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.kick.Kick
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.hasPermission
import me.melijn.melijnbot.objects.translation.MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.objects.translation.MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import java.awt.Color
import java.time.ZoneId

class KickCommand : AbstractCommand("command.kick") {

    init {
        id = 30
        name = "kick"
        commandCategory = CommandCategory.MODERATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val targetMember = retrieveMemberByArgsNMessage(context, 0, true, botAllowed = false) ?: return
        if (!context.guild.selfMember.canInteract(targetMember)) {
            val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                .replace(PLACEHOLDER_USER, targetMember.asTag)
            sendMsg(context, msg)
            return
        }

        if (!context.member.canInteract(targetMember) && !hasPermission(context, SpecialPermission.PUNISH_BYPASS_HIGHER.node, true)) {
            val msg = context.getTranslation(MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION)
                .replace(PLACEHOLDER_USER, targetMember.asTag)
            sendMsg(context, msg)
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
            sendMsgEL(it, kicking)
        }?.firstOrNull()

        continueKicking(context, targetMember, kick, message)
    }

    private suspend fun continueKicking(context: CommandContext, targetMember: Member, kick: Kick, kickingMessage: Message? = null) {
        val guild = context.guild
        val author = context.author
        val language = context.getLanguage()
        val daoManager = context.daoManager
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetMember.idLong)

        val kickedMessageDm = getKickMessage(language, privZoneId, guild, targetMember.user, author, kick)
        val warnedMessageLc = getKickMessage(language, zoneId, guild, targetMember.user, author, kick, true, targetMember.user.isBot, kickingMessage != null)

        context.daoManager.kickWrapper.addKick(kick)
        val msg = try {
            context.guild.kick(targetMember, kick.reason).await()
            kickingMessage?.editMessage(
                kickedMessageDm
            )?.override(true)?.queue()

            val logChannelWrapper = context.daoManager.logChannelWrapper
            val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.KICK)).await()
            val logChannel = guild.getTextChannelById(logChannelId)
            logChannel?.let { it1 -> sendEmbed(context.daoManager.embedDisabledWrapper, it1, warnedMessageLc) }

            context.getTranslation("$root.success")
                .replace(PLACEHOLDER_USER, targetMember.asTag)
                .replace("%reason%", kick.reason)

        } catch (t: Throwable) {
            val failedMsg = context.getTranslation("message.kicking.failed")
            kickingMessage?.editMessage(failedMsg)?.queue()

            context.getTranslation("$root.failure")
                .replace(PLACEHOLDER_USER, targetMember.asTag)
                .replace("%cause%", t.message ?: "/")

        }
        sendMsg(context, msg)
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
    val eb = EmbedBuilder()

    var description = "```LDIF\n"
    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.description.nlc")
            .replace("%serverName%", guild.name)
            .replace("%serverId%", guild.id)
    }

    description += i18n.getTranslation(language, "message.punishment.kick.description")
        .replace("%kickAuthor%", kickAuthor.asTag)
        .replace("%kickAuthorId%", kickAuthor.id)
        .replace("%kicked%", kickedUser.asTag)
        .replace("%kickedId%", kickedUser.id)
        .replace("%reason%", kick.reason)
        .replace("%moment%", (kick.moment.asEpochMillisToDateTime(zoneId)))
        .replace("%kickId%", kick.kickId)

    val extraDesc: String = if (!received || isBot) {
        i18n.getTranslation(language,
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
        .replace(PLACEHOLDER_USER, kickAuthor.asTag)
        .replace("%spaces%", " ".repeat(45).substring(0, 45 - kickAuthor.name.length) + "\u200B")

    eb.setAuthor(author, null, kickAuthor.effectiveAvatarUrl)
    eb.setDescription(description)
    eb.setThumbnail(kickedUser.effectiveAvatarUrl)
    eb.setColor(Color.ORANGE)
    return eb.build()
}