package me.melijn.melijnbot.internals.utils.message

import io.ktor.client.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.StringUtils
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.internal.entities.DataMessage
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO


suspend fun sendRsp(context: CommandContext, image: BufferedImage, extension: String) {
    val premiumGuild = context.isFromGuild && context.daoManager.supporterWrapper.getGuilds().contains(context.guildId)
    if (premiumGuild) {
        sendRsp(context.textChannel, context.daoManager, context.getLanguage(), image, extension)
    } else {
        sendMsg(context, image, extension)
    }
}


suspend fun sendRsp(textChannel: TextChannel, daoManager: DaoManager, language: String, image: BufferedImage, extension: String) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }
    val byteArrayOutputStream = ByteArrayOutputStream()

    byteArrayOutputStream.use { baos ->
        withContext(Dispatchers.IO) {
            ImageIO.write(image, extension, baos)
        }

        sendFileRsp(textChannel, daoManager, language, baos.toByteArray(), extension)
    }
}

suspend fun sendMsg(context: CommandContext, image: BufferedImage, extension: String) {
    if (context.isFromGuild) {
        sendMsg(context.textChannel, image, extension)
    } else {
        sendMsg(context.privateChannel, image, extension)
    }
}

suspend fun sendMsg(privateChannel: PrivateChannel, image: BufferedImage, extension: String) {
    ByteArrayOutputStream().use { baos ->
        withContext(Dispatchers.IO) {
            ImageIO.write(image, extension, baos)
        }

        privateChannel.sendFile(baos.toByteArray(), "finished.$extension").queue()
    }
}

suspend fun sendMsg(textChannel: TextChannel, image: BufferedImage, extension: String) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }

    ByteArrayOutputStream().use { baos ->
        withContext(Dispatchers.IO) {
            ImageIO.write(image, extension, baos)
        }

        textChannel.sendFile(baos.toByteArray(), "finished.$extension").queue()
    }
}


suspend fun sendFileRsp(context: CommandContext, msg: String, bytes: ByteArray, extension: String) {
    val premiumGuild = context.isFromGuild && context.daoManager.supporterWrapper.getGuilds().contains(context.guildId)
    if (premiumGuild) {
        sendFileRsp(context.textChannel, msg, context.daoManager, context.getLanguage(), bytes, extension)
    } else {
        sendFile(context, msg, bytes, extension)
    }
}

suspend fun sendFile(context: CommandContext, msg: String, bytes: ByteArray, extension: String) {
    if (context.isFromGuild) {
        sendFile(context.getLanguage(), msg, context.textChannel, bytes, extension)
    } else {
        sendFile(context.getLanguage(), msg, context.privateChannel, bytes, extension)
    }
}

fun sendFileRsp(textChannel: TextChannel, msg: String, daoManager: DaoManager, language: String, bytes: ByteArray, extension: String) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }

    if (textChannel.guild.maxFileSize < (bytes.size)) {
        val size = StringUtils.humanReadableByteCountBin(bytes.size)
        val max = StringUtils.humanReadableByteCountBin(textChannel.jda.selfUser.allowedFileSize)

        val err = i18n.getTranslation(language, "message.filetoobig")
            .withVariable("size", size)
            .withVariable("max", max)
        sendRsp(textChannel, daoManager, err)
        return
    }

    TaskManager.async(textChannel) {
        val message = textChannel.sendMessage(msg).addFile(bytes, "finished.$extension").awaitOrNull()
            ?: return@async

        handleRspDelete(daoManager, message)
    }
}

suspend fun sendFileRsp(context: CommandContext, bytes: ByteArray, extension: String) {
    val premiumGuild = context.isFromGuild && context.daoManager.supporterWrapper.getGuilds().contains(context.guildId)
    if (premiumGuild) {
        sendFileRsp(context.textChannel, context.daoManager, context.getLanguage(), bytes, extension)
    } else {
        sendFile(context, bytes, extension)
    }
}

fun sendFileRsp(textChannel: TextChannel, daoManager: DaoManager, language: String, bytes: ByteArray, extension: String) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }

    if (textChannel.guild.maxFileSize < (bytes.size)) {
        val size = StringUtils.humanReadableByteCountBin(bytes.size)
        val max = StringUtils.humanReadableByteCountBin(textChannel.jda.selfUser.allowedFileSize)

        val msg = i18n.getTranslation(language, "message.filetoobig")
            .withVariable("size", size)
            .withVariable("max", max)
        sendRsp(textChannel, daoManager, msg)
        return
    }

    TaskManager.async(textChannel) {
        val message = textChannel.sendFile(bytes, "finished.$extension").awaitOrNull()
            ?: return@async

        handleRspDelete(daoManager, message)
    }
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
        val size = StringUtils.humanReadableByteCountBin(bytes.size)
        val max = StringUtils.humanReadableByteCountBin(privateChannel.jda.selfUser.allowedFileSize)
        val msg = i18n.getTranslation(language, "message.filetoobig")
            .withVariable("size", size)
            .withVariable("max", max)
        sendMsg(privateChannel, msg)
        return
    }

    privateChannel.sendFile(bytes, "finished.$extension").queue()
}

fun sendFile(language: String, textChannel: TextChannel, bytes: ByteArray, extension: String) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }

    if (textChannel.guild.maxFileSize < (bytes.size)) {
        val size = StringUtils.humanReadableByteCountBin(bytes.size)
        val max = StringUtils.humanReadableByteCountBin(textChannel.jda.selfUser.allowedFileSize)
        val msg = i18n.getTranslation(language, "message.filetoobig")
            .withVariable("size", size)
            .withVariable("max", max)
        sendMsg(textChannel, msg)
        return
    }

    textChannel.sendFile(bytes, "finished.$extension").queue()
}

fun sendFile(language: String, msg: String, privateChannel: PrivateChannel, bytes: ByteArray, extension: String) {
    if (privateChannel.jda.selfUser.allowedFileSize < (bytes.size)) {
        val size = StringUtils.humanReadableByteCountBin(bytes.size)
        val max = StringUtils.humanReadableByteCountBin(privateChannel.jda.selfUser.allowedFileSize)
        val msg1 = i18n.getTranslation(language, "message.filetoobig")
            .withVariable("size", size)
            .withVariable("max", max)
        sendMsg(privateChannel, msg1)
        return
    }

    privateChannel.sendMessage(msg).addFile(bytes, "finished.$extension").queue()
}

fun sendFile(language: String, msg: String, textChannel: TextChannel, bytes: ByteArray, extension: String) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }

    if (textChannel.guild.maxFileSize < (bytes.size)) {
        val size = StringUtils.humanReadableByteCountBin(bytes.size)
        val max = StringUtils.humanReadableByteCountBin(textChannel.jda.selfUser.allowedFileSize)
        val msg = i18n.getTranslation(language, "message.filetoobig")
            .withVariable("size", size)
            .withVariable("max", max)
        sendMsg(textChannel, msg)
        return
    }

    textChannel.sendMessage(msg).addFile(bytes, "finished.$extension").queue()
}


suspend fun sendMsg(context: CommandContext, listImages: List<BufferedImage>, extension: String) {
    if (context.isFromGuild) {
        sendMsg(context.textChannel, listImages, extension)
    } else {
        sendMsg(context.privateChannel, listImages, extension)
    }
}

suspend fun sendMsg(privateChannel: PrivateChannel, listImages: List<BufferedImage>, extension: String) {
    ByteArrayOutputStream().use { baos ->
        withContext(Dispatchers.IO) {
            for (image in listImages) {
                ImageIO.write(image, extension, baos)
            }
        }

        privateChannel.sendFile(baos.toByteArray(), "finished.$extension").queue()
    }
}

suspend fun sendMsg(textChannel: TextChannel, listImages: List<BufferedImage>, extension: String) {
    require(textChannel.canTalk()) { "Cannot talk in this channel " + textChannel.name }

    ByteArrayOutputStream().use { baos ->
        withContext(Dispatchers.IO) {
            for (image in listImages) {
                ImageIO.write(image, extension, baos)
            }
        }

        textChannel.sendFile(baos.toByteArray(), "finished.$extension").queue()
    }
}

private suspend fun attachmentsAction(textChannel: MessageChannel, httpClient: HttpClient, urls: Map<String, String>): MessageAction? {
    var messageAction: MessageAction? = null
    val guild = if (textChannel is TextChannel) textChannel.guild else null
    for ((index, url) in urls.iterator().withIndex()) {
        val stream = ImageUtils.downloadImage(httpClient, url.key, true, guild, null)
        messageAction = if (index == 0) {
            textChannel.sendFile(stream, url.value)
        } else {
            messageAction?.addFile(stream, url.value)
        }
    }
    return messageAction
}

private suspend fun msgWithAttachmentsAction(channel: MessageChannel, httpClient: HttpClient, message: Message, attachments: Map<String, String>): MessageAction? {
    var messageAction: MessageAction? = null
    val guild = if (channel is TextChannel) channel.guild else null
    for ((index, url) in attachments.iterator().withIndex()) {
        val stream = ImageUtils.downloadImage(httpClient, url.key, true, guild, null)
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
    return messageAction
}


suspend fun sendAttachmentsRspAwaitN(textChannel: TextChannel, httpClient: HttpClient, daoManager: DaoManager, attachments: Map<String, String>): Message? {
    val message = attachmentsAction(textChannel, httpClient, attachments)?.awaitOrNull() ?: return null
    TaskManager.async(textChannel) {
        handleRspDelete(daoManager, message)
    }
    return message
}

suspend fun sendRspWithAttachmentsAwaitN(textChannel: TextChannel, httpClient: HttpClient, daoManager: DaoManager, message: Message, attachments: Map<String, String>): Message? {
    val msg = msgWithAttachmentsAction(textChannel, httpClient, message, attachments)?.awaitOrNull() ?: return null
    TaskManager.async(textChannel) {
        handleRspDelete(daoManager, msg)
    }
    return msg
}

suspend fun sendRspAwaitN(channel: TextChannel, daoManager: DaoManager, msg: Message): Message? {
    require(channel.canTalk()) {
        "Cannot talk in this channel: #(${channel.name}, ${channel.id}) - ${channel.guild.id}"
    }

    var action = if (msg.contentRaw.isNotBlank()) channel.sendMessage(msg.contentRaw) else null
    for (embed in msg.embeds) {
        if (action == null) action = channel.sendMessage(embed)
        else action.embed(embed)
    }

    if (msg is DataMessage) {
        action?.allowedMentions(msg.allowedMentions)
    }

    val message = action?.awaitOrNull() ?: return null
    TaskManager.async(channel) {
        handleRspDelete(daoManager, message)
    }
    return message
}

suspend fun sendAttachments(textChannel: MessageChannel, httpClient: HttpClient, urls: Map<String, String>) {
    attachmentsAction(textChannel, httpClient, urls)?.queue()
}

fun sendRspAttachments(daoManager: DaoManager, httpClient: HttpClient, textChannel: TextChannel, urls: Map<String, String>) {
    TaskManager.async(textChannel) {
        val message = attachmentsAction(textChannel, httpClient, urls)?.awaitOrNull() ?: return@async

        handleRspDelete(daoManager, message)
    }
}

fun sendRspWithAttachments(daoManager: DaoManager, httpClient: HttpClient, textChannel: TextChannel, message: Message, attachments: Map<String, String>) {
    TaskManager.async(textChannel) {
        val msg = msgWithAttachmentsAction(textChannel, httpClient, message, attachments)?.awaitOrNull() ?: return@async

        handleRspDelete(daoManager, msg)
    }
}

suspend fun sendMsgWithAttachments(channel: MessageChannel, httpClient: HttpClient, message: Message, attachments: Map<String, String>) {
    msgWithAttachmentsAction(channel, httpClient, message, attachments)?.queue()
}

suspend fun sendAttachmentsAwaitN(textChannel: MessageChannel, httpClient: HttpClient, urls: Map<String, String>): Message? {
    return attachmentsAction(textChannel, httpClient, urls)?.awaitOrNull()
}

suspend fun sendMsgWithAttachmentsAwaitN(channel: MessageChannel, httpClient: HttpClient, message: Message, attachments: Map<String, String>): Message? {
    return msgWithAttachmentsAction(channel, httpClient, message, attachments)?.awaitOrNull()
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

    byteArrayOutputStream.use { baos ->
        withContext(Dispatchers.IO) {
            ImageIO.write(image, extension, baos)
        }

        textChannel
            .sendMessage(msg)
            .addFile(baos.toByteArray(), "finished.$extension")
            .awaitOrNull()?.let { msg ->
                messageList.add(
                    msg
                )
            }
    }

    return messageList
}

suspend fun sendMsgAwaitEL(privateChannel: PrivateChannel, msg: String, image: BufferedImage?, extension: String): List<Message> {
    if (privateChannel.user.isBot) return emptyList()

    val messageList = mutableListOf<Message>()
    val byteArrayOutputStream = ByteArrayOutputStream()

    byteArrayOutputStream.use { baos ->
        withContext(Dispatchers.IO) {
            ImageIO.write(image, extension, baos)
        }
        privateChannel
            .sendMessage(msg)
            .addFile(baos.toByteArray(), "finished.$extension")
            .awaitOrNull()?.let { msg ->
                messageList.add(
                    msg
                )
            }
    }
    return messageList
}