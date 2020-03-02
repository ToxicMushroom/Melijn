package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.warn.Warn
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import java.awt.Color
import java.time.ZoneId

class WarnCommand : AbstractCommand("command.warn") {

    init {
        id = 32
        name = "warn"
        commandCategory = CommandCategory.MODERATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val targetMember = getMemberByArgsNMessage(context, 0, true, botAllowed = false) ?: return

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
            sendMsgEL(it, warning)
        }?.firstOrNull()

        continueWarning(context, targetMember, warn, message)
    }

    private suspend fun continueWarning(context: CommandContext, targetMember: Member, warn: Warn, warningMessage: Message? = null) {
        val guild = context.guild
        val author = context.author
        val daoManager = context.daoManager
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, targetMember.idLong)
        val language = context.getLanguage()
        val warnedMessageDm = getWarnMessage(language, privZoneId, guild, targetMember.user, author, warn)
        val warnedMessageLc = getWarnMessage(language, zoneId, guild, targetMember.user, author, warn, true, targetMember.user.isBot, warningMessage != null)

        context.daoManager.warnWrapper.addWarn(warn)

        warningMessage?.editMessage(
            warnedMessageDm
        )?.override(true)?.queue()

        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.WARN)
        logChannel?.let { it1 ->
            sendEmbed(daoManager.embedDisabledWrapper, it1, warnedMessageLc)
        }

        val msg = context.getTranslation("$root.success")
            .replace(PLACEHOLDER_USER, targetMember.asTag)
            .replace("%reason%", warn.reason)
        sendMsg(context, msg)
    }
}

fun getWarnMessage(
    language: String,
    zoneId: ZoneId,
    guild: Guild,
    warnedUser: User,
    warnAuthor: User,
    warn: Warn,
    lc: Boolean = false,
    isBot: Boolean = false,
    received: Boolean = true
): MessageEmbed {
    val eb = EmbedBuilder()

    var description = "```LDIF\n"
    if (!lc) {
        description += i18n.getTranslation(language, "message.punishment.description.nlc")
            .replace("%guildName%", guild.name)
            .replace("%guildId%", guild.id)
    }

    description += i18n.getTranslation(language, "message.punishment.warn.description")
        .replace("%warnAuthor%", warnAuthor.asTag)
        .replace("%warnAuthorId%", warnAuthor.id)
        .replace("%warned%", warnedUser.asTag)
        .replace("%warnedId%", warnedUser.id)
        .replace("%reason%", warn.reason)
        .replace("%moment%", (warn.moment.asEpochMillisToDateTime(zoneId)))
        .replace("%warnId%", warn.warnId)

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

    val author = i18n.getTranslation(language, "message.punishment.warn.author")
        .replace(PLACEHOLDER_USER, warnAuthor.asTag)
        .replace("%spaces%", " ".repeat(45).substring(0, 45 - warnAuthor.name.length) + "\u200B")

    eb.setAuthor(author, null, warnAuthor.effectiveAvatarUrl)
    eb.setDescription(description)
    eb.setThumbnail(warnedUser.effectiveAvatarUrl)
    eb.setColor(Color.YELLOW)
    return eb.build()
}