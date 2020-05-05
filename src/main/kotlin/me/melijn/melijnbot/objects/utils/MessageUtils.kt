package me.melijn.melijnbot.objects.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.database.embed.EmbedDisabledWrapper
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.StringUtils.humanReadableByteCountBin
import me.melijn.melijnbot.objects.utils.StringUtils.toBase64
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.slf4j.LoggerFactory
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

val logger = LoggerFactory.getLogger("messageutils")

fun Throwable.sendInGuild(context: CommandContext, thread: Thread = Thread.currentThread(), extra: String? = null) = runBlocking {
    sendInGuildSuspend(context.guildN, context.messageChannel, context.author, thread, "Message: ${MarkdownSanitizer.escape(context.message.contentRaw)}\n" + (extra
        ?: "")
    )
}


fun Throwable.sendInGuild(guild: Guild? = null, channel: MessageChannel? = null, author: User? = null, thread: Thread = Thread.currentThread(), extra: String? = null) = runBlocking {
    sendInGuildSuspend(guild, channel, author, thread, extra)
}

suspend fun Throwable.sendInGuildSuspend(guild: Guild? = null, channel: MessageChannel? = null, author: User? = null, thread: Thread = Thread.currentThread(), extra: String? = null) {
    if (Container.instance.settings.unLoggedThreads.contains(thread.name)) return

    val channelId = Container.instance.settings.exceptionChannel
    val textChannel = MelijnBot.shardManager.getTextChannelById(channelId) ?: return

    val caseId = System.currentTimeMillis().toBase64()

    val sb = StringBuilder()

    sb.appendln("**CaseID**: $caseId")
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
    extra?.let {
        sb.appendln("**Extra**")
        sb.appendln(it)
    }

    if (Container.instance.logToDiscord)
        sendMsg(textChannel, sb.toString())

    if (channel != null && (channel !is TextChannel || channel.canTalk()) && (channel is TextChannel || channel is PrivateChannel)) {
        val lang = getLanguage(Container.instance.daoManager, author?.idLong ?: -1, guild?.idLong ?: -1)
        val msg = i18n.getTranslation(lang, "message.exception")
            .replace("%caseId%", caseId)

        if (channel is TextChannel)
            sendMsg(channel, msg)
        else if (channel is PrivateChannel)
            sendMsg(channel, msg)
    }
}

suspend fun sendSyntax(context: CommandContext, translationPath: String = context.commandOrder.last().syntax) {
    val syntax = context.getTranslation("message.command.usage")
        .replace("%syntax%", context.getTranslation(translationPath)
            .replace(PLACEHOLDER_PREFIX, context.usedPrefix)
        )
    sendMsg(context, syntax)
}

suspend fun sendMsgCodeBlock(context: CommandContext, msg: String, lang: String, shouldPaginate: Boolean = false) {
    if (context.isFromGuild) {
        val channel = context.textChannel
        if (!channel.canTalk()) return
        if (msg.length <= 2000) {
            channel.sendMessage(msg).queue()
        } else {
            val parts = StringUtils.splitMessage(msg, maxLength = 2000 - (8 + lang.length) - if (shouldPaginate) 100 else 0)
            if (shouldPaginate && parts.size > 1) {
                val paginatedParts = parts.mapIndexed { index, s ->
                    when {
                        index == 0 -> "$s```\nPage ${index + 1}/${parts.size}"
                        index + 1 == parts.size -> "```$lang\n$s\nPage ${index + 1}/${parts.size}"
                        else -> "```$lang\n$s```\nPage ${index + 1}/${parts.size}"
                    }
                }.toMutableList()

                val message = channel.sendMessage(paginatedParts[0]).await()
                registerPaginationMessage(channel
                    , message, paginatedParts, 0)
            } else {
                parts.forEachIndexed { index, msgPart ->
                    channel.sendMessage(when {
                        index == 0 -> "$msgPart```"
                        index + 1 == parts.size -> "```$lang\n$msgPart"
                        else -> "```$lang\n$msgPart```"
                    }).queue()
                }
            }
        }

    } else {
        val privateChannel = context.privateChannel
        if (msg.length <= 2000) {
            privateChannel.sendMessage(msg).queue()
        } else {
            val parts = StringUtils.splitMessage(msg, maxLength = 2000 - (8 + lang.length))
            if (shouldPaginate && parts.size > 1) {
                val paginatedParts = parts.mapIndexed { index, s ->
                    when {
                        index == 0 -> "$s```\nPage ${index + 1}/${parts.size}"
                        index + 1 == parts.size -> "```$lang\n$s\nPage ${index + 1}/${parts.size}"
                        else -> "```$lang\n$s```\nPage ${index + 1}/${parts.size}"
                    }
                }.toMutableList()

                val message = privateChannel.sendMessage(paginatedParts[0]).await()
                registerPaginationMessage(privateChannel, message, paginatedParts, 0)
            } else {
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
}

suspend fun sendMsgCodeBlocks(
    context: CommandContext,
    msg: String,
    lang: String,
    success: ((message: Message) -> Unit)? = null,
    failed: ((ex: Throwable) -> Unit)? = null
) {
    if (context.isFromGuild) sendMsgCodeBlocks(context.textChannel, msg, lang, success, failed)
    else sendMsgCodeBlocks(context.privateChannel, msg, lang, success, failed)
}

suspend fun sendMsgCodeBlocks(
    channel: PrivateChannel,
    msg: String,
    lang: String,
    success: ((message: Message) -> Unit)? = null,
    failed: ((ex: Throwable) -> Unit)? = null
) {
    if (msg.length <= 2000) {
        channel.sendMessage(msg).queue(success, failed)
    } else {

        val list = StringUtils.splitMessageWithCodeBlocks(msg, 1600, 20 + lang.length, lang)
            .toMutableList()
        val rsp = sendPaginationMsg(channel, list, 0)
        success?.invoke(rsp)
    }
}

suspend fun sendMsgCodeBlocks(
    channel: TextChannel,
    msg: String,
    lang: String,
    success: ((message: Message) -> Unit)? = null,
    failed: ((ex: Throwable) -> Unit)? = null
) {
    if (channel.canTalk()) {
        if (msg.length <= 2000) {
            channel.sendMessage(msg).queue(success, failed)
        } else {

            val list = StringUtils.splitMessageWithCodeBlocks(msg, 1600, 20 + lang.length, lang)
                .toMutableList()
            val rsp = sendPaginationMsg(channel, list, 0)
            success?.invoke(rsp)
        }
    }
}

fun escapeForLog(string: String): String {
    return string
        .replace("`", "´")
        .replace("\n", " ")
        .trim()
}

suspend fun sendAttachments(textChannel: MessageChannel, urls: Map<String, String>): Message = suspendCoroutine {
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

suspend fun sendMsgWithAttachments(channel: MessageChannel, message: Message, attachments: Map<String, String>): Message = suspendCoroutine {
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
        failed?.invoke(t)
    }
}

suspend fun sendEmbed(privateChannel: PrivateChannel, embed: MessageEmbed): List<Message> = suspendCoroutine {
    runBlocking {
        try {
            if (privateChannel.user.isBot) {
                it.resume(emptyList())
                return@runBlocking
            }
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
        sb.appendln("__${this.title}__\n")
    }
    if (this.description != null) {
        sb.append(this.description?.replace(Regex("\\[(.+)]\\((.+)\\)"), "$1 (Link: $2)")).append("\n\n")
    }
    if (this.image != null) {
        sb.append(this.image?.url).append("\n\n")
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
            sendFile(context.getLanguage(), context.textChannel, bytes, extension, success, failed)
        } else {
            sendFile(context.getLanguage(), context.privateChannel, bytes, extension, success, failed)
        }
    }
}


suspend fun sendFile(language: String, privateChannel: PrivateChannel, bytes: ByteArray, extension: String, success: ((List<Message>) -> Unit)? = null, failed: ((Throwable) -> Unit)? = null) {
    try {
        val messageList = mutableListOf<Message>()
        if (privateChannel.jda.selfUser.allowedFileSize < (bytes.size)) {
            val size = humanReadableByteCountBin(bytes.size)
            val max = humanReadableByteCountBin(privateChannel.jda.selfUser.allowedFileSize)
            val msg = i18n.getTranslation(language, "message.filetoobig")
                .replace("%size%", size)
                .replace("%max%", max)
            sendMsg(privateChannel, msg)
            return
        }
        messageList.add(privateChannel.sendFile(bytes, "finished.$extension").await())

        success?.invoke(messageList)
    } catch (t: Throwable) {
        t.printStackTrace()
        failed?.invoke(t)
        return
    }
}

suspend fun sendFile(language: String, textChannel: TextChannel, bytes: ByteArray, extension: String, success: ((List<Message>) -> Unit)? = null, failed: ((Throwable) -> Unit)? = null) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }
    try {
        val messageList = mutableListOf<Message>()
        if (textChannel.jda.selfUser.allowedFileSize < (bytes.size)) {
            val size = humanReadableByteCountBin(bytes.size)
            val max = humanReadableByteCountBin(textChannel.jda.selfUser.allowedFileSize)
            val msg = i18n.getTranslation(language, "message.filetoobig")
                .replace("%size%", size)
                .replace("%max%", max)
            sendMsg(textChannel, msg)
            return
        }
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


suspend fun sendMsg(privateChannel: PrivateChannel, msg: ModularMessage): Message? {
    val message: Message? = msg.toMessage()
    return when {
        message == null -> sendAttachments(privateChannel, msg.attachments)
        msg.attachments.isNotEmpty() -> sendMsgWithAttachments(privateChannel, message, msg.attachments)
        else -> sendMsg(privateChannel, message)
    }
}

suspend fun sendMsg(textChannel: TextChannel, msg: ModularMessage): Message? {
    val message: Message? = msg.toMessage()
    return when {
        message == null -> sendAttachments(textChannel, msg.attachments)
        msg.attachments.isNotEmpty() -> sendMsgWithAttachments(textChannel, message, msg.attachments)
        else -> sendMsg(textChannel, message)
    }
}


suspend fun sendPaginationMsg(context: CommandContext, msgList: MutableList<String>, index: Int): List<Message> = suspendCoroutine {
    val msg = msgList[index]
    if (msg.length > 2000) throw IllegalArgumentException("No splitting here :angry:")

    runBlocking {
        if (context.isFromGuild) {
            val message = sendMsg(context.textChannel, msg).first()
            registerPaginationMessage(context.textChannel, message, msgList, index)
        } else {
            val message = sendMsg(context.privateChannel, msg).first()
            registerPaginationMessage(context.privateChannel, message, msgList, index)
        }
    }
}

suspend fun sendPaginationMsg(textChannel: TextChannel, msgList: MutableList<String>, index: Int): Message = suspendCoroutine {
    val msg = msgList[index]
    if (msg.length > 2000) throw IllegalArgumentException("No splitting here :angry:")

    runBlocking {
        val message = sendMsg(textChannel, msg).first()
        it.resume(message)
        registerPaginationMessage(textChannel, message, msgList, index)
    }
}

suspend fun sendPaginationMsg(privateChannel: PrivateChannel, msgList: MutableList<String>, index: Int): Message = suspendCoroutine {
    val msg = msgList[index]
    if (msg.length > 2000) throw IllegalArgumentException("No splitting here :angry:")

    runBlocking {
        val message = sendMsg(privateChannel, msg).first()
        it.resume(message)
        registerPaginationMessage(privateChannel, message, msgList, index)
    }
}

suspend fun sendPaginationModularMsg(context: CommandContext, msgList: MutableList<ModularMessage>, index: Int): List<Message> = suspendCoroutine {
    val msg = msgList[index]

    runBlocking {
        if (context.isFromGuild) {
            val message = sendMsg(context.textChannel, msg)
                ?: throw IllegalArgumentException("Couldn't send the message")
            registerPaginationModularMessage(context.textChannel, message, msgList, index)
        } else {
            val message = sendMsg(context.privateChannel, msg)
                ?: throw IllegalArgumentException("Couldn't send the message")
            registerPaginationModularMessage(context.privateChannel, message, msgList, index)
        }
    }
}

suspend fun sendPaginationModularMsg(textChannel: TextChannel, modularList: MutableList<ModularMessage>, index: Int): Message = suspendCoroutine {
    val msg = modularList[index]

    runBlocking {
        val message = sendMsg(textChannel, msg)
            ?: throw IllegalArgumentException("Couldn't send the message")
        it.resume(message)
        registerPaginationModularMessage(textChannel, message, modularList, index)
    }
}

suspend fun sendPaginationModularMsg(privateChannel: PrivateChannel, msgList: MutableList<ModularMessage>, index: Int): Message = suspendCoroutine {
    val msg = msgList[index]

    runBlocking {
        val message = sendMsg(privateChannel, msg)
            ?: throw IllegalArgumentException("Couldn't send the message")
        it.resume(message)
        registerPaginationModularMessage(privateChannel, message, msgList, index)
    }
}


fun registerPaginationModularMessage(textChannel: TextChannel, message: Message, msgList: MutableList<ModularMessage>, index: Int) {
    Container.instance.modularPaginationMap[System.nanoTime()] = ModularPaginationInfo(
        textChannel.guild.idLong,
        textChannel.idLong,
        message.idLong,
        msgList,
        index
    )

    addPaginationEmotes(message)
}

fun registerPaginationModularMessage(privateChannel: PrivateChannel, message: Message, msgList: MutableList<ModularMessage>, index: Int) {
    Container.instance.modularPaginationMap[System.nanoTime()] = ModularPaginationInfo(
        -1,
        privateChannel.idLong,
        message.idLong,
        msgList,
        index
    )

    addPaginationEmotes(message)
}

fun registerPaginationMessage(textChannel: TextChannel, message: Message, msgList: MutableList<String>, index: Int) {
    Container.instance.paginationMap[System.nanoTime()] = PaginationInfo(
        textChannel.guild.idLong,
        textChannel.idLong,
        message.idLong,
        msgList,
        index
    )

    addPaginationEmotes(message)
}

fun registerPaginationMessage(privateChannel: PrivateChannel, message: Message, msgList: MutableList<String>, index: Int) {
    Container.instance.paginationMap[System.nanoTime()] = PaginationInfo(
        -1,
        privateChannel.idLong,
        message.idLong,
        msgList,
        index
    )

    addPaginationEmotes(message)
}

fun addPaginationEmotes(message: Message) {
    message.addReaction("⏪").queue()
    message.addReaction("◀️").queue()
    message.addReaction("▶️").queue()
    message.addReaction("⏩").queue()
}

suspend fun sendMsg(privateChannel: PrivateChannel, msg: String): List<Message> = suspendCoroutine {
    val success = { success: List<Message> -> it.resume(success) }
    val failed = { failed: Throwable -> it.resumeWithException(failed) }
    runBlocking {
        sendMsg(privateChannel, msg, success, failed)
    }
}

//Returns empty message list on failures
suspend fun sendMsgEL(privateChannel: PrivateChannel, msg: String): List<Message> = suspendCoroutine {
    val success = { success: List<Message> -> it.resume(success) }
    val failed = { _: Throwable -> it.resume(emptyList()) }
    runBlocking {
        sendMsg(privateChannel, msg, success, failed)
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

                if (msg.contains("%")) {
                    logger.warn("raw variable ?: $msg")
                }

                messageList.add(channel.sendMessage(msg).await())
            } else {
                val msgParts = StringUtils.splitMessage(msg).withIndex()
                for ((index, text) in msgParts) {

                    if (msg.contains("%")) {
                        logger.warn("raw variable ?: $msg")
                    }

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
    require(channel.canTalk()) {
        "Cannot talk in this channel: #(${channel.name}, ${channel.id}) - ${channel.guild.id}"
    }
    var action = if (msg.contentRaw.isNotBlank()) channel.sendMessage(msg.contentRaw) else null
    for (embed in msg.embeds) {
        if (action == null) action = channel.sendMessage(embed)
        else action.embed(embed)
    }
    action?.queue(success, failed)
}

fun sendMsg(channel: PrivateChannel, msg: Message, success: ((messages: Message) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    var action = if (msg.contentRaw.isNotBlank()) channel.sendMessage(msg.contentRaw) else null
    for (embed in msg.embeds) {
        if (action == null) action = channel.sendMessage(embed)
        else action.embed(embed)
    }
    action?.queue(success, failed)
}

suspend fun sendMsg(channel: TextChannel, msg: Message): Message? = suspendCoroutine {
    require(channel.canTalk()) {
        "Cannot talk in this channel: #(${channel.name}, ${channel.id}) - ${channel.guild.id}"
    }
    var action = if (msg.contentRaw.isNotBlank()) channel.sendMessage(msg.contentRaw) else null
    for (embed in msg.embeds) {
        if (action == null) action = channel.sendMessage(embed)
        else action.embed(embed)
    }
    if (action == null) {
        it.resume(null)
    }
    action?.queue({ msg ->
        it.resume(msg)
    }, { _ ->
        it.resume(null)
    })
}

suspend fun sendMsg(channel: PrivateChannel, msg: Message): Message? = suspendCoroutine {
    var action = if (msg.contentRaw.isNotBlank()) channel.sendMessage(msg.contentRaw) else null
    for (embed in msg.embeds) {
        if (action == null) action = channel.sendMessage(embed)
        else action.embed(embed)
    }
    if (action == null) {
        it.resume(null)
    }
    action?.queue({ msg ->
        it.resume(msg)
    }, { _ ->
        it.resume(null)
    })
}

suspend fun sendMsg(context: CommandContext, msg: String, bufferedImage: BufferedImage?, extension: String): List<Message> = suspendCoroutine {
    runBlocking {
        it.resume(if (context.isFromGuild) {
            sendMsg(context.textChannel, msg, bufferedImage, extension)
        } else {
            sendMsg(context.privateChannel, msg, bufferedImage, extension)
        })
    }
}

suspend fun sendMsg(textChannel: TextChannel, msg: String, image: BufferedImage?, extension: String): List<Message> = suspendCoroutine {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }
    try {
        val messageList = mutableListOf<Message>()
        val byteArrayOutputStream = ByteArrayOutputStream()

        ImageIO.write(image, extension, byteArrayOutputStream)

        runBlocking {
            messageList.add(
                textChannel
                    .sendMessage(msg)
                    .addFile(byteArrayOutputStream.toByteArray(), "finished.$extension")
                    .await()
            )
        }

        it.resume(messageList)
    } catch (t: Throwable) {
        t.printStackTrace()
        it.resumeWithException(t)
    }
}

suspend fun sendMsg(privateChannel: PrivateChannel, msg: String, image: BufferedImage?, extension: String): List<Message> = suspendCoroutine {
    try {
        val messageList = mutableListOf<Message>()
        val byteArrayOutputStream = ByteArrayOutputStream()

        ImageIO.write(image, extension, byteArrayOutputStream)

        runBlocking {
            messageList.add(
                privateChannel
                    .sendMessage(msg)
                    .addFile(byteArrayOutputStream.toByteArray(), "finished.$extension")
                    .await()
            )
        }

        it.resume(messageList)
    } catch (t: Throwable) {
        t.printStackTrace()
        it.resumeWithException(t)
    }
}

suspend fun sendFeatureRequiresPremiumMessage(context: CommandContext, featurePath: String, featureReplaceMap: Map<String, String> = emptyMap()) {
    var feature = context.getTranslation(featurePath)
    for ((key, replacement) in featureReplaceMap) {
        feature = feature.replace("%$key%", replacement)
    }

    val baseMsg = context.getTranslation("message.feature.requires.premium")
        .replace("%feature%", feature)
    sendMsg(context, baseMsg)
}

suspend fun sendFeatureRequiresGuildPremiumMessage(context: CommandContext, featurePath: String, featureReplaceMap: Map<String, String> = emptyMap()) {
    var feature = context.getTranslation(featurePath)
    for ((key, replacement) in featureReplaceMap) {
        feature = feature.replace("%$key%", replacement)
    }

    val baseMsg = context.getTranslation("message.feature.requires.premium.server")
        .replace("%feature%", feature)
    sendMsg(context, baseMsg)
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
    return this.replace(PLACEHOLDER_PREFIX, context.usedPrefix)
}

fun String.replacePrefix(prefix: String): String {
    return this.replace(PLACEHOLDER_PREFIX, prefix)
}

fun getNicerUsedPrefix(settings: Settings, prefix: String): String {
    return if (prefix.contains(settings.id.toString()) && USER_MENTION.matcher(prefix).matches()) {
        "@${settings.name} "
    } else {
        prefix
    }
}