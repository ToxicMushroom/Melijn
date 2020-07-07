package me.melijn.melijnbot.objects.utils.message

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.StringUtils.toBase64
import me.melijn.melijnbot.objects.utils.toUCC
import me.melijn.melijnbot.objects.utils.withVariable
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import java.io.PrintWriter
import java.io.StringWriter


fun Throwable.sendInGuild(context: CommandContext, thread: Thread = Thread.currentThread(), extra: String? = null) = runBlocking {
    val sanitizedMessage = "Message: ${MarkdownSanitizer.escape(context.message.contentRaw)}\n" + (extra ?: "")
    sendInGuildSuspend(context.guildN, context.messageChannel, context.author, thread, sanitizedMessage)
}

fun Throwable.sendInGuild(
    guild: Guild? = null,
    channel: MessageChannel? = null,
    author: User? = null,
    thread: Thread = Thread.currentThread(),
    extra: String? = null
) = runBlocking {
    sendInGuildSuspend(guild, channel, author, thread, extra)
}

suspend fun Throwable.sendInGuildSuspend(
    guild: Guild? = null,
    channel: MessageChannel? = null,
    author: User? = null,
    thread: Thread = Thread.currentThread(),
    extra: String? = null
) {
    if (Container.instance.settings.unLoggedThreads.contains(thread.name)) return

    val channelId = Container.instance.settings.exceptionChannel
    val textChannel = MelijnBot.shardManager.getTextChannelById(channelId) ?: return

    val caseId = System.currentTimeMillis().toBase64()

    val sb = StringBuilder()

    sb.append("**CaseID**: ").appendln(caseId)
    if (guild != null) {
        sb.append("**Guild**: ").append(guild.name).append(" | ").appendln(guild.id)
    }
    if (channel != null) {
        sb.append("**")
            .append(channel.type.toUCC())
            .append("Channel**: #").append(channel.name).append(" | ").appendln(channel.id)
    }
    if (author != null) {
        sb.append("**User**: ").append(author.asTag).append(" | ").appendln(author.id)
    }
    sb.append("**Thread**: ").appendln(thread.name)

    val writer = StringWriter()
    val printWriter = PrintWriter(writer)
    this.printStackTrace(printWriter)
    val stacktrace = MarkdownSanitizer.escape(writer.toString())
        .replace("at me.melijn.melijnbot", "**at me.melijn.melijnbot**")
    sb.append(stacktrace)
    extra?.let {
        sb.appendln("**Extra**")
        sb.appendln(it)
    }

    if (Container.instance.logToDiscord) {
        sendMsg(textChannel, sb.toString())
    }

    if (channel != null && (channel !is TextChannel || channel.canTalk()) && (channel is TextChannel || channel is PrivateChannel)) {
        val lang = getLanguage(Container.instance.daoManager, author?.idLong ?: -1, guild?.idLong ?: -1)
        val msg = i18n.getTranslation(lang, "message.exception")
            .withVariable("caseId", caseId)

        if (channel is TextChannel)
            sendMsg(channel, msg)
        else if (channel is PrivateChannel)
            sendMsg(channel, msg)
    }
}