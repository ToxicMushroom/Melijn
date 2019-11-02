package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.warn.Warn
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import java.awt.Color

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
        val targetMember = getMemberByArgsNMessage(context, 0) ?: return

        val language = context.getLanguage()
        if (!context.getGuild().selfMember.canInteract(targetMember)) {
            val msg = i18n.getTranslation(language, "$root.cannotwarn")
                .replace(PLACEHOLDER_USER, targetMember.asTag)
            sendMsg(context, msg)
            return
        }

        var reason = context.rawArg
            .replaceFirst(context.args[0], "")
            .trim()
        if (reason.isBlank()) reason = "/"

        reason = reason.trim()

        val warn = Warn(
            context.getGuildId(),
            targetMember.idLong,
            context.authorId,
            reason
        )

        val warning = i18n.getTranslation(language, "message.warning..")

        try {
            val privateChannel = targetMember.user.openPrivateChannel().await()
            val message = privateChannel.sendMessage(warning).await()

            continueWarning(context, targetMember, warn, message)
        } catch (t: Throwable) {
            continueWarning(context, targetMember, warn)
        }
    }

    private suspend fun continueWarning(context: CommandContext, targetMember: Member, warn: Warn, warningMessage: Message? = null) {
        val guild = context.getGuild()
        val author = context.getAuthor()

        val language = context.getLanguage()
        val warnedMessageDm = getWarnMessage(language, guild, targetMember.user, author, warn)
        val warnedMessageLc = getWarnMessage(language, guild, targetMember.user, author, warn, true, targetMember.user.isBot, warningMessage != null)

        context.daoManager.warnWrapper.addWarn(warn)

        warningMessage?.editMessage(
            warnedMessageDm
        )?.override(true)?.queue()

        val logChannelWrapper = context.daoManager.logChannelWrapper
        val logChannelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.WARN)).await()
        val logChannel = guild.getTextChannelById(logChannelId)
        logChannel?.let { it1 ->
            sendEmbed(context.daoManager.embedDisabledWrapper, it1, warnedMessageLc)
        }

        val msg = i18n.getTranslation(language, "$root.success")
            .replace(PLACEHOLDER_USER, targetMember.asTag)
            .replace("%reason%", warn.reason)
        sendMsg(context, msg)
    }
}

fun getWarnMessage(
    language: String,
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
        .replace("%moment%", (warn.moment.asEpochMillisToDateTime()))

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