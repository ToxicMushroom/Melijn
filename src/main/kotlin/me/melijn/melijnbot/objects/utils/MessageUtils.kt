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

val logger = LoggerFactory.getLogger("messageutils")

fun Throwable.sendInGuild(context: CommandContext, thread: Thread = Thread.currentThread(), extra: String? = null) = runBlocking {
    sendInGuildSuspend(context.guildN, context.messageChannel, context.author, thread, "Message: ${MarkdownSanitizer.escape(context.message.contentRaw)}\n" + (extra
        ?: "")
    )
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

                val message = channel.sendMessage(paginatedParts[0]).awaitOrNull()
                message?.let { registerPaginationMessage(channel, it, paginatedParts, 0) }
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

                val message = privateChannel.sendMessage(paginatedParts[0]).awaitOrNull()
                message?.let { registerPaginationMessage(privateChannel, it, paginatedParts, 0) }
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
    lang: String
) {
    if (context.isFromGuild) sendMsgCodeBlocks(context.textChannel, msg, lang)
    else sendMsgCodeBlocks(context.privateChannel, msg, lang)
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
        val rsp = sendPaginationMsgAwait(channel, list, 0)
        success?.invoke(rsp)
    }
}

suspend fun sendMsgCodeBlocks(
    channel: TextChannel,
    msg: String,
    lang: String
) {
    if (!channel.canTalk()) throw IllegalArgumentException("bro cringe fix perms")
    if (msg.length <= 2000) {
        channel.sendMessage(msg).queue()
    } else {

        val list = StringUtils.splitMessageWithCodeBlocks(msg, 1600, 20 + lang.length, lang)
            .toMutableList()
        sendPaginationMsg(channel, list, 0)
    }
}

fun escapeForLog(string: String): String {
    return string
        .replace("`", "´")
        .replace("\n", " ")
        .trim()
}

suspend fun sendAttachmentsAwaitN(textChannel: MessageChannel, urls: Map<String, String>): Message? {
    var messageAction: MessageAction? = null
    for ((index, url) in urls.iterator().withIndex()) {
        val stream = URL(url.key).openStream()
        messageAction = if (index == 0) {
            textChannel.sendFile(stream, url.value)
        } else {
            messageAction?.addFile(stream, url.value)
        }
    }
    return messageAction?.awaitOrNull()
}

suspend fun sendMsgWithAttachmentsAwaitN(channel: MessageChannel, message: Message, attachments: Map<String, String>): Message? {
    var messageAction: MessageAction? = null
    for ((index, url) in attachments.iterator().withIndex()) {
        val stream = URL(url.key).openStream()
        messageAction = if (index == 0) {
            var action = if (message.contentRaw.isNotBlank()) {
                channel.sendMessage(message.contentRaw)
            } else {
                null
            }
            for (embed in message.embeds) {
                if (action == null) action = channel.sendMessage(embed)
                else action.embed(embed)
            }
            action
        } else {
            messageAction
        }?.addFile(stream, url.value)
    }
    return messageAction?.awaitOrNull()
}

fun sendEmbed(context: CommandContext, embed: MessageEmbed) {
    if (context.isFromGuild) {
        sendEmbed(context.daoManager.embedDisabledWrapper, context.textChannel, embed)
    } else {
        sendEmbed(context.privateChannel, embed)
    }
}

suspend fun sendEmbedAwaitEL(context: CommandContext, embed: MessageEmbed): List<Message> {
    return if (context.isFromGuild) {
        sendEmbedAwaitEL(context.daoManager.embedDisabledWrapper, context.textChannel, embed)
    } else {
        sendEmbedAwaitEL(context.privateChannel, embed)
    }
}

fun sendEmbed(privateChannel: PrivateChannel, embed: MessageEmbed) {
    if (privateChannel.user.isBot) return
    privateChannel.sendMessage(embed).queue()
}

suspend fun sendEmbedAwaitEL(privateChannel: PrivateChannel, embed: MessageEmbed): List<Message> {
    if (privateChannel.user.isBot) {
        return emptyList()
    }
    val msg = privateChannel.sendMessage(embed).awaitOrNull()
    return msg?.let { listOf(it) } ?: emptyList()
}


suspend fun sendEmbedAwaitEL(embedDisabledWrapper: EmbedDisabledWrapper, textChannel: TextChannel, embed: MessageEmbed): List<Message> {
    val guild = textChannel.guild
    if (!textChannel.canTalk()) {
        throw IllegalArgumentException("No permission to talk in this channel")
    }

    return if (guild.selfMember.hasPermission(textChannel, Permission.MESSAGE_EMBED_LINKS) &&
        !embedDisabledWrapper.embedDisabledCache.contains(guild.idLong)) {

        val msg = textChannel.sendMessage(embed).awaitOrNull()
        msg?.let { listOf(it) } ?: emptyList()

    } else {
        sendEmbedAsMessageAwaitEL(textChannel, embed)
    }
}

fun sendEmbed(embedDisabledWrapper: EmbedDisabledWrapper, textChannel: TextChannel, embed: MessageEmbed) {
    val guild = textChannel.guild
    if (!textChannel.canTalk()) {
        throw IllegalArgumentException("No permission to talk in this channel")
    }
    if (guild.selfMember.hasPermission(textChannel, Permission.MESSAGE_EMBED_LINKS) &&
        !embedDisabledWrapper.embedDisabledCache.contains(guild.idLong)) {
        textChannel.sendMessage(embed).queue()
    } else {
        sendEmbedAsMessage(textChannel, embed)
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

fun sendEmbedAsMessage(textChannel: TextChannel, embed: MessageEmbed) {
    sendMsg(textChannel, embed.toMessage())
}

suspend fun sendEmbedAsMessageAwaitEL(textChannel: TextChannel, embed: MessageEmbed): List<Message> {
    return sendMsgAwaitEL(textChannel, embed.toMessage())
}

suspend fun sendMsg(context: CommandContext, image: BufferedImage, extension: String) {
    if (context.isFromGuild) {
        sendMsg(context.textChannel, image, extension)
    } else {
        sendMsg(context.privateChannel, image, extension)
    }
}

suspend fun sendMsg(privateChannel: PrivateChannel, image: BufferedImage, extension: String) {
    val byteArrayOutputStream = ByteArrayOutputStream()

    withContext(Dispatchers.IO) {
        ImageIO.write(image, extension, byteArrayOutputStream)
    }

    privateChannel.sendFile(byteArrayOutputStream.toByteArray(), "finished.$extension").queue()
}

suspend fun sendMsg(textChannel: TextChannel, image: BufferedImage, extension: String) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }

    val byteArrayOutputStream = ByteArrayOutputStream()
    withContext(Dispatchers.IO) {
        ImageIO.write(image, extension, byteArrayOutputStream)
    }

    textChannel.sendFile(byteArrayOutputStream.toByteArray(), "finished.$extension").queue()
}

suspend fun sendFile(context: CommandContext, bytes: ByteArray, extension: String) {
    if (context.isFromGuild) {
        sendFile(context.getLanguage(), context.textChannel, bytes, extension)
    } else {
        sendFile(context.getLanguage(), context.privateChannel, bytes, extension)
    }
}


fun sendFile(language: String, privateChannel: PrivateChannel, bytes: ByteArray, extension: String) {
    if (privateChannel.jda.selfUser.allowedFileSize < (bytes.size)) {
        val size = humanReadableByteCountBin(bytes.size)
        val max = humanReadableByteCountBin(privateChannel.jda.selfUser.allowedFileSize)
        val msg = i18n.getTranslation(language, "message.filetoobig")
            .replace("%size%", size)
            .replace("%max%", max)
        sendMsg(privateChannel, msg)
        return
    }

    privateChannel.sendFile(bytes, "finished.$extension").queue()
}

fun sendFile(language: String, textChannel: TextChannel, bytes: ByteArray, extension: String) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }

    if (textChannel.guild.maxFileSize < (bytes.size)) {
        val size = humanReadableByteCountBin(bytes.size)
        val max = humanReadableByteCountBin(textChannel.jda.selfUser.allowedFileSize)
        val msg = i18n.getTranslation(language, "message.filetoobig")
            .replace("%size%", size)
            .replace("%max%", max)
        sendMsg(textChannel, msg)
        return
    }

    textChannel.sendFile(bytes, "finished.$extension").queue()
}


suspend fun sendMsg(context: CommandContext, listImages: List<BufferedImage>, extension: String) {
    if (context.isFromGuild) {
        sendMsg(context.textChannel, listImages, extension)
    } else {
        sendMsg(context.privateChannel, listImages, extension)
    }
}

suspend fun sendMsg(privateChannel: PrivateChannel, listImages: List<BufferedImage>, extension: String) {
    val byteArrayOutputStream = ByteArrayOutputStream()

    withContext(Dispatchers.IO) {
        for (image in listImages) {
            ImageIO.write(image, extension, byteArrayOutputStream)
        }
    }

    privateChannel.sendFile(byteArrayOutputStream.toByteArray(), "finished.$extension").queue()
}

suspend fun sendMsg(textChannel: TextChannel, listImages: List<BufferedImage>, extension: String) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }

    val byteArrayOutputStream = ByteArrayOutputStream()
    withContext(Dispatchers.IO) {
        for (image in listImages) {
            ImageIO.write(image, extension, byteArrayOutputStream)
        }
    }

    textChannel.sendFile(byteArrayOutputStream.toByteArray(), "finished.$extension").queue()
}


fun sendMsg(context: CommandContext, msg: String) {
    if (context.isFromGuild) {
        sendMsg(context.textChannel, msg)
    } else {
        sendMsg(context.privateChannel, msg)
    }
}


suspend fun sendMsgAwaitN(privateChannel: PrivateChannel, msg: ModularMessage): Message? {
    val message: Message? = msg.toMessage()
    return when {
        message == null -> sendAttachmentsAwaitN(privateChannel, msg.attachments)
        msg.attachments.isNotEmpty() -> sendMsgWithAttachmentsAwaitN(privateChannel, message, msg.attachments)
        else -> sendMsgAwaitN(privateChannel, message)
    }
}

suspend fun sendMsgAwaitN(textChannel: TextChannel, msg: ModularMessage): Message? {
    val message: Message? = msg.toMessage()
    return when {
        message == null -> sendAttachmentsAwaitN(textChannel, msg.attachments)
        msg.attachments.isNotEmpty() -> sendMsgWithAttachmentsAwaitN(textChannel, message, msg.attachments)
        else -> sendMsgAwaitN(textChannel, message)
    }
}

suspend fun sendMsg(textChannel: TextChannel, msg: ModularMessage) {
    val message: Message? = msg.toMessage()
    when {
        message == null -> sendAttachmentsAwaitN(textChannel, msg.attachments)
        msg.attachments.isNotEmpty() -> sendMsgWithAttachmentsAwaitN(textChannel, message, msg.attachments)
        else -> sendMsgAwaitN(textChannel, message)
    }
}


suspend fun sendPaginationMsg(context: CommandContext, msgList: MutableList<String>, index: Int) {
    val msg = msgList[index]
    if (msg.length > 2000) throw IllegalArgumentException("No splitting here :angry:")

    if (context.isFromGuild) {
        val message = sendMsgAwaitEL(context.textChannel, msg).first()
        registerPaginationMessage(context.textChannel, message, msgList, index)
    } else {
        val message = sendMsgAwaitEL(context.privateChannel, msg).first()
        registerPaginationMessage(context.privateChannel, message, msgList, index)
    }
}

suspend fun sendPaginationMsg(textChannel: TextChannel, msgList: MutableList<String>, index: Int) {
    val msg = msgList[index]
    if (msg.length > 2000) throw IllegalArgumentException("No splitting here :angry:")

    val message = sendMsgAwaitEL(textChannel, msg).first()
    registerPaginationMessage(textChannel, message, msgList, index)
}

suspend fun sendPaginationMsg(privateChannel: PrivateChannel, msgList: MutableList<String>, index: Int) {
    val msg = msgList[index]
    if (msg.length > 2000) throw IllegalArgumentException("No splitting here :angry:")

    val message = sendMsgAwaitEL(privateChannel, msg).first()
    registerPaginationMessage(privateChannel, message, msgList, index)
}

suspend fun sendPaginationMsgAwait(textChannel: TextChannel, msgList: MutableList<String>, index: Int): Message {
    val msg = msgList[index]
    if (msg.length > 2000) throw IllegalArgumentException("No splitting here :angry:")

    val message = sendMsgAwaitEL(textChannel, msg).first()
    registerPaginationMessage(textChannel, message, msgList, index)
    return message
}

suspend fun sendPaginationMsgAwait(privateChannel: PrivateChannel, msgList: MutableList<String>, index: Int): Message {
    val msg = msgList[index]
    if (msg.length > 2000) throw IllegalArgumentException("No splitting here :angry:")

    val message = sendMsgAwaitEL(privateChannel, msg).first()
    registerPaginationMessage(privateChannel, message, msgList, index)
    return message
}

suspend fun sendPaginationModularMsg(context: CommandContext, msgList: MutableList<ModularMessage>, index: Int) {
    val msg = msgList[index]

    if (context.isFromGuild) {
        val message = sendMsgAwaitN(context.textChannel, msg)
            ?: throw IllegalArgumentException("Couldn't send the message")
        registerPaginationModularMessage(context.textChannel, message, msgList, index)
    } else {
        val message = sendMsgAwaitN(context.privateChannel, msg)
            ?: throw IllegalArgumentException("Couldn't send the message")
        registerPaginationModularMessage(context.privateChannel, message, msgList, index)
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

    addPaginationEmotes(message, msgList.size > 2)
}

fun registerPaginationModularMessage(privateChannel: PrivateChannel, message: Message, msgList: MutableList<ModularMessage>, index: Int) {
    Container.instance.modularPaginationMap[System.nanoTime()] = ModularPaginationInfo(
        -1,
        privateChannel.idLong,
        message.idLong,
        msgList,
        index
    )

    addPaginationEmotes(message, msgList.size > 2)
}

fun registerPaginationMessage(textChannel: TextChannel, message: Message, msgList: MutableList<String>, index: Int) {
    Container.instance.paginationMap[System.nanoTime()] = PaginationInfo(
        textChannel.guild.idLong,
        textChannel.idLong,
        message.idLong,
        msgList,
        index
    )

    addPaginationEmotes(message, msgList.size > 2)
}

fun registerPaginationMessage(privateChannel: PrivateChannel, message: Message, msgList: MutableList<String>, index: Int) {
    Container.instance.paginationMap[System.nanoTime()] = PaginationInfo(
        -1,
        privateChannel.idLong,
        message.idLong,
        msgList,
        index
    )

    addPaginationEmotes(message, msgList.size > 2)
}

fun addPaginationEmotes(message: Message, morePages: Boolean) {
    if (morePages) message.addReaction("⏪").queue()
    message.addReaction("◀️").queue()
    message.addReaction("▶️").queue()
    if (morePages) message.addReaction("⏩").queue()
}

suspend fun sendMsgAwaitEL(privateChannel: PrivateChannel, msg: String): List<Message> {
    val messageList = mutableListOf<Message>()
    if (privateChannel.user.isBot) return emptyList()
    if (msg.length <= 2000) {
        privateChannel.sendMessage(msg).awaitOrNull()?.let { messageList.add(it) }
    } else {
        val msgParts = StringUtils.splitMessage(msg).withIndex()
        for ((index, text) in msgParts) {
            privateChannel.sendMessage(text).awaitOrNull()?.let { messageList.add(index, it) }
        }
    }

    return messageList
}

fun sendMsg(privateChannel: PrivateChannel, msg: String) {
    if (privateChannel.user.isBot) return
    if (msg.length <= 2000) {
        privateChannel.sendMessage(msg).queue()
    } else {
        val msgParts = StringUtils.splitMessage(msg)
        for (text in msgParts) {
            privateChannel.sendMessage(text).queue()
        }
    }
}

suspend fun sendMsgAwaitEL(context: CommandContext, msg: String): List<Message> {
    return if (context.isFromGuild) {
        sendMsgAwaitEL(context.textChannel, msg)
    } else {
        sendMsgAwaitEL(context.privateChannel, msg)
    }
}


suspend fun sendMsgAwaitEL(channel: TextChannel, msg: String): List<Message> {
    require(channel.canTalk()) { "Cannot talk in this channel " + channel.name }

    val messageList = mutableListOf<Message>()
    if (msg.length <= 2000) {

        if (msg.contains("%")) {
            logger.warn("raw variable ?: $msg")
        }

        channel.sendMessage(msg).awaitOrNull()?.let { messageList.add(it) }
    } else {
        val msgParts = StringUtils.splitMessage(msg).withIndex()
        for ((index, text) in msgParts) {

            if (msg.contains("%")) {
                logger.warn("raw variable ?: $msg")
            }

            channel.sendMessage(text).awaitOrNull()?.let { messageList.add(index, it) }
        }
    }

    return messageList
}

fun sendMsg(channel: TextChannel, msg: String) {
    require(channel.canTalk()) { "Cannot talk in this channel " + channel.name }

    if (msg.length <= 2000) {
        channel.sendMessage(msg).queue()
    } else {
        val msgParts = StringUtils.splitMessage(msg)

        for (text in msgParts) {
            channel.sendMessage(text).queue()
        }
    }
}

suspend fun sendMsg(channel: TextChannel, msg: String, success: ((messages: List<Message>) -> Unit)? = null, failed: ((ex: Throwable) -> Unit)? = null) {
    require(channel.canTalk()) {
        "Cannot talk in this channel: #(${channel.name}, ${channel.id}) - ${channel.guild.id}"
    }
    try {
        val messageList = mutableListOf<Message>()
        if (msg.length <= 2000) {
            channel.sendMessage(msg).awaitOrNull()?.let { messageList.add(it) }
        } else {
            val msgParts = StringUtils.splitMessage(msg).withIndex()
            for ((index, text) in msgParts) {
                channel.sendMessage(text).awaitOrNull()?.let { messageList.add(index, it) }
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

suspend fun sendMsgAwaitN(channel: TextChannel, msg: Message): Message? {
    require(channel.canTalk()) {
        "Cannot talk in this channel: #(${channel.name}, ${channel.id}) - ${channel.guild.id}"
    }
    var action = if (msg.contentRaw.isNotBlank()) channel.sendMessage(msg.contentRaw) else null
    for (embed in msg.embeds) {
        if (action == null) action = channel.sendMessage(embed)
        else action.embed(embed)
    }

    return action?.awaitOrNull()
}

suspend fun sendMsgAwaitN(channel: PrivateChannel, msg: Message): Message? {
    var action = if (msg.contentRaw.isNotBlank()) {
        channel.sendMessage(msg.contentRaw)
    } else null
    for (embed in msg.embeds) {
        if (action == null) action = channel.sendMessage(embed)
        else action.embed(embed)
    }

    return action?.awaitOrNull()
}

suspend fun sendMsgAwaitEL(context: CommandContext, msg: String, bufferedImage: BufferedImage?, extension: String): List<Message> {
    return if (context.isFromGuild) {
        sendMsgAwaitEL(context.textChannel, msg, bufferedImage, extension)
    } else {
        sendMsgAwaitEL(context.privateChannel, msg, bufferedImage, extension)
    }
}

suspend fun sendMsgAwaitEL(textChannel: TextChannel, msg: String, image: BufferedImage?, extension: String): List<Message> {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }

    val messageList = mutableListOf<Message>()
    val byteArrayOutputStream = ByteArrayOutputStream()

    ImageIO.write(image, extension, byteArrayOutputStream)

    textChannel
        .sendMessage(msg)
        .addFile(byteArrayOutputStream.toByteArray(), "finished.$extension")
        .awaitOrNull()?.let {
            messageList.add(
                it
            )
        }

    byteArrayOutputStream.close()

    return messageList
}

suspend fun sendMsgAwaitEL(privateChannel: PrivateChannel, msg: String, image: BufferedImage?, extension: String): List<Message> {

    val messageList = mutableListOf<Message>()
    val byteArrayOutputStream = ByteArrayOutputStream()

    ImageIO.write(image, extension, byteArrayOutputStream)

    privateChannel
        .sendMessage(msg)
        .addFile(byteArrayOutputStream.toByteArray(), "finished.$extension")
        .awaitOrNull()?.let {
            messageList.add(
                it
            )
        }

    byteArrayOutputStream.close()

    return messageList
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