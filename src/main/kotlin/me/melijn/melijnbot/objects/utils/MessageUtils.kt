package me.melijn.melijnbot.objects.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.database.embed.EmbedDisabledWrapper
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PREFIX_PLACE_HOLDER
import me.melijn.melijnbot.objects.translation.i18n
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URL
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

fun Throwable.sendInGuild(context: CommandContext, thread: Thread = Thread.currentThread()) = runBlocking {
    sendInGuildSuspend(context.guild, context.messageChannel, context.author, thread)
}


fun Throwable.sendInGuild(guild: Guild? = null, channel: MessageChannel? = null, author: User? = null, thread: Thread = Thread.currentThread()) = runBlocking {
    sendInGuildSuspend(guild, channel, author, thread)
}

suspend fun Throwable.sendInGuildSuspend(guild: Guild? = null, channel: MessageChannel? = null, author: User? = null, thread: Thread = Thread.currentThread()) {
    if (Container.instance.settings.unLoggedThreads.contains(thread.name)) return

    val channelId = Container.instance.settings.exceptionChannel
    val textChannel = MelijnBot.shardManager.getTextChannelById(channelId) ?: return

    val sb = StringBuilder()
    if (guild != null) {
        sb.appendln("**Guild**: " + guild.name + " | " + guild.id)
    }
    if (channel != null) {
        sb.appendln("**" + channel.type.name.toUpperWordCase() + "Channel**: #" + channel.name + " | " + channel.id)
    }
    if (author != null) {
        sb.appendln("**User**: " + author.asTag + " | " + author.id)
    }
    sb.appendln("**Thread**: " + thread.name)
    val writer = StringWriter()
    val printWriter = PrintWriter(writer)
    this.printStackTrace(printWriter)
    val stacktrace = MarkdownSanitizer.escape(writer.toString())
        .replace("at me.melijn.melijnbot", "**at me.melijn.melijnbot**")
    sb.append(stacktrace)
    sendMsg(textChannel, sb.toString())
}

suspend fun sendSyntax(context: CommandContext, translationPath: String = context.commandOrder.last().syntax) {
    val language = context.getLanguage()
    val syntax = i18n.getTranslation(language, "message.command.usage")
        .replace("%syntax%", i18n.getTranslation(language, translationPath)
            .replace("%prefix%", context.usedPrefix))
    sendMsg(context.textChannel, syntax)
}

fun sendMsgCodeBlock(context: CommandContext, msg: String, lang: String) {
    if (context.isFromGuild) {
        val channel = context.textChannel
        if (!channel.canTalk()) return
        if (msg.length <= 2000) {
            channel.sendMessage(msg).queue()
        } else {
            val parts = StringUtils.splitMessage(msg, margin = 8 + lang.length);
            parts.forEachIndexed { index, msgPart ->
                channel.sendMessage(when {
                    index == 0 -> "$msgPart```"
                    index + 1 == parts.size -> "```$lang\n$msgPart"
                    else -> "```$lang\n$msgPart```"
                }).queue()
            }
        }

    } else {
        val privateChannel = context.privateChannel
        if (msg.length <= 2000) {
            privateChannel.sendMessage(msg).queue()
        } else {
            val parts = StringUtils.splitMessage(msg, margin = 8 + lang.length);
            parts.forEachIndexed { index, msgPart ->
                privateChannel.sendMessage(when {
                    index == 0 -> "$msgPart```"
                    index + 1 == parts.size -> "```$lang\n$msgPart"
                    else -> "```$lang\n$msgPart```"
                }).queue()
            }
        }
    }
}

fun sendMsgCodeBlocks(
    context: CommandContext,
    msg: String,
    lang: String,
    success: ((message: Message) -> Unit)? = null,
    failed: ((ex: Throwable) -> Unit)? = null,
    multicallback: Boolean = false
) {
    if (context.isFromGuild) sendMsgCodeBlocks(context.textChannel, msg, lang, success, failed, multicallback)
    else sendMsgCodeBlocks(context.privateChannel, msg, lang, success, failed, multicallback)
}

fun sendMsgCodeBlocks(
    channel: PrivateChannel,
    msg: String,
    lang: String,
    success: ((message: Message) -> Unit)? = null,
    failed: ((ex: Throwable) -> Unit)? = null,
    multicallback: Boolean = false
) {
    if (msg.length <= 2000) {
        channel.sendMessage(msg).queue(success, failed)
    } else {
        var executedOnce = false
        StringUtils.splitMessageWithCodeBlocks(msg, 1600, 20 + lang.length, lang).forEach {
            val future = channel.sendMessage(it)
            if (executedOnce && !multicallback) {
                future.queue()
            } else {
                future.queue(success, failed)
            }
            executedOnce = true
        }
    }
}

fun sendMsgCodeBlocks(
    channel: TextChannel,
    msg: String,
    lang: String,
    success: ((message: Message) -> Unit)? = null,
    failed: ((ex: Throwable) -> Unit)? = null,
    multicallback: Boolean = false
) {
    if (channel.canTalk()) {
        if (msg.length <= 2000) {
            channel.sendMessage(msg).queue(success, failed)
        } else {
            var executedOnce = false
            StringUtils.splitMessageWithCodeBlocks(msg, 1600, 20 + lang.length, lang).forEach {
                val future = channel.sendMessage(it)
                if (executedOnce && !multicallback) {
                    future.queue()
                } else {
                    future.queue(success, failed)
                }
                executedOnce = true
            }
        }
    }
}

fun escapeForLog(string: String): String {
    return string.replace("`", "´")
        .replace("\n", " ")
}

suspend fun sendAttachments(textChannel: TextChannel, urls: Map<String, String>): Message = suspendCoroutine {
    val success = { success: Message -> it.resume(success) }
    val failed = { failed: Throwable -> it.resumeWithException(failed) }
    var messageAction: MessageAction? = null
    for ((index, url) in urls.iterator().withIndex()) {
        val stream = URL(url.key).openStream()
        messageAction = if (index == 0) {
            textChannel.sendFile(stream, url.value)
        } else {
            messageAction?.addFile(stream, url.value)
        }
    }
    messageAction?.queue(success, failed)
}

suspend fun sendMsgWithAttachments(channel: TextChannel, message: Message, attachments: Map<String, String>): Message = suspendCoroutine {
    val success = { success: Message -> it.resume(success) }
    val failed = { failed: Throwable -> it.resumeWithException(failed) }
    var messageAction: MessageAction? = null
    for ((index, url) in attachments.iterator().withIndex()) {
        val stream = URL(url.key).openStream()
        messageAction = if (index == 0) {
            var action = if (message.contentRaw.isNotBlank()) channel.sendMessage(message.contentRaw) else null
            for (embed in message.embeds) {
                if (action == null) action = channel.sendMessage(embed)
                else action.embed(embed)
            }
            action
        } else {
            messageAction
        }?.addFile(stream, url.value)
    }
    messageAction?.queue(success, failed)
}

suspend fun sendEmbed(context: CommandContext, embed: MessageEmbed, success: ((messages: List<Message>) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    if (context.isFromGuild) {
        sendEmbed(context.daoManager.embedDisabledWrapper, context.textChannel, embed, success, failed)
    } else {
        sendEmbed(context.privateChannel, embed, success, failed)
    }
}

suspend fun sendEmbed(context: CommandContext, embed: MessageEmbed): List<Message> {
    return if (context.isFromGuild) {
        sendEmbed(context.daoManager.embedDisabledWrapper, context.textChannel, embed)
    } else {
        sendEmbed(context.privateChannel, embed)
    }
}

suspend fun sendEmbed(privateChannel: PrivateChannel, embed: MessageEmbed, success: ((messages: List<Message>) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    try {
        val msg = privateChannel.sendMessage(embed).await()
        success?.invoke(listOf(msg))
    } catch (t: Throwable) {
        t.printStackTrace()
        failed?.invoke(t)
    }
}

suspend fun sendEmbed(privateChannel: PrivateChannel, embed: MessageEmbed): List<Message> = suspendCoroutine {
    runBlocking {
        try {
            val msg = privateChannel.sendMessage(embed).await()
            it.resume(listOf(msg))
        } catch (t: Throwable) {
            it.resumeWithException(t)
        }
    }
}


suspend fun sendEmbed(embedDisabledWrapper: EmbedDisabledWrapper, textChannel: TextChannel, embed: MessageEmbed): List<Message> = suspendCoroutine {
    runBlocking {
        val guild = textChannel.guild
        if (!textChannel.canTalk()) {
            it.resumeWithException(IllegalArgumentException("No permission to talk in this channel"))
            return@runBlocking
        }
        if (guild.selfMember.hasPermission(textChannel, Permission.MESSAGE_EMBED_LINKS) &&
            !embedDisabledWrapper.embedDisabledCache.contains(guild.idLong)) {
            try {
                val msg = textChannel.sendMessage(embed).await()
                it.resume(listOf(msg))
            } catch (t: Throwable) {
                it.resumeWithException(t)
            }
        } else {
            it.resume(sendEmbedAsMessage(textChannel, embed))
        }
    }
}

suspend fun sendEmbed(embedDisabledWrapper: EmbedDisabledWrapper, textChannel: TextChannel, embed: MessageEmbed, success: ((messages: List<Message>) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    val guild = textChannel.guild
    if (!textChannel.canTalk()) {
        failed?.invoke(IllegalArgumentException("No permission to talk in this channel"))
        return
    }
    if (guild.selfMember.hasPermission(textChannel, Permission.MESSAGE_EMBED_LINKS) &&
        !embedDisabledWrapper.embedDisabledCache.contains(guild.idLong)) {
        try {
            val msg = textChannel.sendMessage(embed).await()
            success?.invoke(listOf(msg))
        } catch (t: Throwable) {
            t.printStackTrace()
            failed?.invoke(t)
        }
    } else {
        sendEmbedAsMessage(textChannel, embed, success, failed)
    }
}

fun MessageEmbed.toMessage(): String {
    val sb = StringBuilder()
    if (this.author != null) {
        sb.append("***").append(this.author?.name).appendln("***")
    }
    if (this.title != null) {
        sb.appendln("**__${this.title}__**\n")
    }
    if (this.description != null) {
        sb.append(this.description?.replace(Regex("\\[(.+)]\\((.+)\\)"), "$1 (Link: $2)")).append("\n\n")
    }
    if (this.fields.isNotEmpty()) {
        for (field in this.fields) {
            sb.append("**").append(field.name).append("**\n")
                .append(field.value?.replace(Regex("\\[(.+)]\\((.+)\\)"), "$1 (Link: $2)")).append("\n\n")
        }
    }
    if (this.footer != null) {
        sb.append("*${this.footer?.text}")
        if (this.timestamp != null)
            sb.append(" | ")
        else sb.append("*")
    }
    if (this.timestamp != null) {
        sb.append(this.timestamp?.format(DateTimeFormatter.ISO_DATE_TIME)).append("*")
    }
    return sb.toString()
}

suspend fun sendEmbedAsMessage(textChannel: TextChannel, embed: MessageEmbed, success: ((messages: List<Message>) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    sendMsg(textChannel, embed.toMessage(), success, failed)
}

suspend fun sendEmbedAsMessage(textChannel: TextChannel, embed: MessageEmbed): List<Message> {
    return sendMsg(textChannel, embed.toMessage())
}

suspend fun sendMsg(context: CommandContext, msg: String, success: ((messages: List<Message>) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    if (context.isFromGuild) sendMsg(context.textChannel, msg, success, failed)
    else sendMsg(context.privateChannel, msg, success, failed)
}


suspend fun sendMsg(context: CommandContext, image: BufferedImage, extension: String): List<Message> = suspendCoroutine {
    val success = { success: List<Message> -> it.resume(success) }
    val failed = { failed: Throwable -> it.resumeWithException(failed) }
    runBlocking {
        if (context.isFromGuild) {
            sendMsg(context.textChannel, image, extension, success, failed)
        } else {
            sendMsg(context.privateChannel, image, extension, success, failed)
        }
    }
}

suspend fun sendMsg(privateChannel: PrivateChannel, image: BufferedImage, extension: String, success: ((List<Message>) -> Unit)? = null, failed: ((Throwable) -> Unit)? = null) {
    try {
        val messageList = mutableListOf<Message>()

        val byteArrayOutputStream = ByteArrayOutputStream()
        withContext(Dispatchers.IO) {
            ImageIO.write(image, extension, byteArrayOutputStream)
        }
        messageList.add(privateChannel.sendFile(byteArrayOutputStream.toByteArray(), "finished.$extension").await())

        success?.invoke(messageList)
    } catch (t: Throwable) {
        t.printStackTrace()
        failed?.invoke(t)
        return
    }
}

suspend fun sendMsg(textChannel: TextChannel, image: BufferedImage, extension: String, success: ((List<Message>) -> Unit)? = null, failed: ((Throwable) -> Unit)? = null) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }
    try {
        val messageList = mutableListOf<Message>()

        val byteArrayOutputStream = ByteArrayOutputStream()
        withContext(Dispatchers.IO) {
            ImageIO.write(image, extension, byteArrayOutputStream)
        }
        messageList.add(textChannel.sendFile(byteArrayOutputStream.toByteArray(), "finished.$extension").await())

        success?.invoke(messageList)
    } catch (t: Throwable) {
        t.printStackTrace()
        failed?.invoke(t)
        return
    }
}

suspend fun sendFile(context: CommandContext, bytes: ByteArray, extension: String): List<Message> = suspendCoroutine {
    val success = { success: List<Message> -> it.resume(success) }
    val failed = { failed: Throwable -> it.resumeWithException(failed) }
    runBlocking {
        if (context.isFromGuild) {
            sendFile(context.textChannel, bytes, extension, success, failed)
        } else {
            sendFile(context.privateChannel, bytes, extension, success, failed)
        }
    }
}


suspend fun sendFile(privateChannel: PrivateChannel, bytes: ByteArray, extension: String, success: ((List<Message>) -> Unit)? = null, failed: ((Throwable) -> Unit)? = null) {
    try {
        val messageList = mutableListOf<Message>()
        messageList.add(privateChannel.sendFile(bytes, "finished.$extension").await())

        success?.invoke(messageList)
    } catch (t: Throwable) {
        t.printStackTrace()
        failed?.invoke(t)
        return
    }
}

suspend fun sendFile(textChannel: TextChannel, bytes: ByteArray, extension: String, success: ((List<Message>) -> Unit)? = null, failed: ((Throwable) -> Unit)? = null) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }
    try {
        val messageList = mutableListOf<Message>()
        messageList.add(textChannel.sendFile(bytes, "finished.$extension").await())

        success?.invoke(messageList)
    } catch (t: Throwable) {
        t.printStackTrace()
        failed?.invoke(t)
        return
    }
}


suspend fun sendMsg(context: CommandContext, listImages: List<BufferedImage>, extension: String): List<Message> = suspendCoroutine {
    val success = { success: List<Message> -> it.resume(success) }
    val failed = { failed: Throwable -> it.resumeWithException(failed) }
    runBlocking {
        if (context.isFromGuild) {
            sendMsg(context.textChannel, listImages, extension, success, failed)
        } else {
            sendMsg(context.privateChannel, listImages, extension, success, failed)
        }
    }
}

suspend fun sendMsg(privateChannel: PrivateChannel, listImages: List<BufferedImage>, extension: String, success: ((List<Message>) -> Unit)? = null, failed: ((Throwable) -> Unit)? = null) {
    try {
        val messageList = mutableListOf<Message>()

        val byteArrayOutputStream = ByteArrayOutputStream()
        withContext(Dispatchers.IO) {
            for (image in listImages) {
                ImageIO.write(image, extension, byteArrayOutputStream)
            }
        }
        messageList.add(privateChannel.sendFile(byteArrayOutputStream.toByteArray(), "finished.$extension").await())

        success?.invoke(messageList)
    } catch (t: Throwable) {
        t.printStackTrace()
        failed?.invoke(t)
        return
    }
}

suspend fun sendMsg(textChannel: TextChannel, listImages: List<BufferedImage>, extension: String, success: ((List<Message>) -> Unit)? = null, failed: ((Throwable) -> Unit)? = null) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }
    try {
        val messageList = mutableListOf<Message>()

        val byteArrayOutputStream = ByteArrayOutputStream()
        withContext(Dispatchers.IO) {
            for (image in listImages) {
                ImageIO.write(image, extension, byteArrayOutputStream)
            }
        }
        messageList.add(textChannel.sendFile(byteArrayOutputStream.toByteArray(), "finished.$extension").await())

        success?.invoke(messageList)
    } catch (t: Throwable) {
        t.printStackTrace()
        failed?.invoke(t)
        return
    }
}


suspend fun sendMsg(context: CommandContext, msg: String): List<Message> = suspendCoroutine {
    val success = { success: List<Message> -> it.resume(success) }
    val failed = { failed: Throwable -> it.resumeWithException(failed) }
    runBlocking {
        if (context.isFromGuild) {
            sendMsg(context.textChannel, msg, success, failed)
        } else {
            sendMsg(context.privateChannel, msg, success, failed)
        }
    }
}

suspend fun sendMsg(privateChannel: PrivateChannel, msg: String, success: ((messages: List<Message>) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    try {
        val messageList = mutableListOf<Message>()
        if (msg.length <= 2000) {
            messageList.add(privateChannel.sendMessage(msg).await())
        } else {
            val msgParts = StringUtils.splitMessage(msg).withIndex()
            for ((index, text) in msgParts) {
                messageList.add(index, privateChannel.sendMessage(text).await())
            }

        }
        success?.invoke(messageList)
    } catch (t: Throwable) {
        t.printStackTrace()
        failed?.invoke(t)
        return
    }
}

suspend fun sendMsg(channel: TextChannel, msg: String): List<Message> = suspendCoroutine {
    runBlocking {
        require(channel.canTalk()) { "Cannot talk in this channel " + channel.name }
        try {
            val messageList = mutableListOf<Message>()
            if (msg.length <= 2000) {
                messageList.add(channel.sendMessage(msg).await())
            } else {
                val msgParts = StringUtils.splitMessage(msg).withIndex()
                for ((index, text) in msgParts) {
                    messageList.add(index, channel.sendMessage(text).await())
                }

            }
            it.resume(messageList)
        } catch (t: Throwable) {
            t.printStackTrace()
            it.resumeWithException(t)
        }
    }
}

suspend fun sendMsg(channel: TextChannel, msg: String, success: ((messages: List<Message>) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    require(channel.canTalk()) { "Cannot talk in this channel " + channel.name }
    try {
        val messageList = mutableListOf<Message>()
        if (msg.length <= 2000) {
            messageList.add(channel.sendMessage(msg).await())
        } else {
            val msgParts = StringUtils.splitMessage(msg).withIndex()
            for ((index, text) in msgParts) {
                messageList.add(index, channel.sendMessage(text).await())
            }

        }
        success?.invoke(messageList)
    } catch (t: Throwable) {
        t.printStackTrace()
        failed?.invoke(t)
        return
    }
}

fun sendMsg(channel: TextChannel, msg: Message, success: ((messages: Message) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    require(channel.canTalk()) { "Cannot talk in this channel " + channel.name }
    var action = if (msg.contentRaw.isNotBlank()) channel.sendMessage(msg.contentRaw) else null
    for (embed in msg.embeds) {
        if (action == null) action = channel.sendMessage(embed)
        else action.embed(embed)
    }
    action?.queue(success, failed)
}

fun String.toUpperWordCase(): String {
    var previous = ' '
    var newString = ""
    this.toCharArray().forEach { c: Char ->
        newString += if (previous == ' ') c.toUpperCase() else c.toLowerCase()
        previous = c
    }
    return newString
}

fun String.replacePrefix(context: CommandContext): String {
    return this.replace(PREFIX_PLACE_HOLDER, context.commandParts[0])
}