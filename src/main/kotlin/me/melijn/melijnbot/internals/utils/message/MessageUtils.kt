package me.melijn.melijnbot.internals.utils.message

import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.database.supporter.SupporterWrapper
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.StringUtils
import me.melijn.melijnbot.internals.utils.USER_MENTION
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.internal.entities.DataMessage

suspend fun sendSyntax(context: CommandContext, translationPath: String = context.commandOrder.last().syntax) {
    val syntax = context.getTranslation("message.command.usage")
        .withVariable("syntax", context.getTranslation(translationPath)
            .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
        )
    sendRsp(context, syntax)
}

fun escapeForLog(string: String): String {
    return string
        .replace("`", "´")
        .replace("\n", " ")
        .trim()
}


fun sendMsg(context: CommandContext, msg: String) {
    if (context.isFromGuild) {
        sendMsg(context.textChannel, msg)
    } else {
        sendMsg(context.privateChannel, msg)
    }
}

fun canResponse(messageChannel: MessageChannel, supporterWrapper: SupporterWrapper): Boolean {
    return if (messageChannel is TextChannel)
        supporterWrapper.guildSupporterIds.contains(messageChannel.guild.idLong)
    else false
}

fun sendRsp(context: CommandContext, msg: String) {
    if (canResponse(context.messageChannel, context.daoManager.supporterWrapper)) {
        sendRsp(context.textChannel, context.daoManager, msg)
    } else {
        sendMsg(context, msg)
    }
}


fun sendRsp(channel: TextChannel, daoManager: DaoManager, msg: String) {
    require(channel.canTalk()) { "Cannot talk in this channel " + channel.name }

    if (msg.length <= 2000) {
        channel.sendMessage(msg).queue { message ->
            TaskManager.async(channel) {
                handleRspDelete(daoManager, message)
            }
        }
    } else {
        val msgParts = StringUtils.splitMessage(msg)

        TaskManager.async(channel) {
            val msgList = mutableListOf<Message>()
            for (text in msgParts) {
                val oneMessage = channel.sendMessage(text).awaitOrNull() ?: continue
                msgList.add(oneMessage)
            }

            handleRspDelete(daoManager, msgList)
        }
    }
}


fun sendRsp(textChannel: TextChannel, context: CommandContext, msg: ModularMessage) {
    if (canResponse(textChannel, context.daoManager.supporterWrapper)) {
        sendRsp(textChannel, context.daoManager, msg)
    } else {
        sendMsg(textChannel, msg)
    }
}

fun sendRspOrMsg(textChannel: TextChannel, daoManager: DaoManager, msg: String) {
    if (canResponse(textChannel, daoManager.supporterWrapper)) {
        sendRsp(textChannel, daoManager, msg)
    } else {
        sendMsg(textChannel, msg)
    }
}

fun sendRsp(channel: TextChannel, daoManager: DaoManager, msg: ModularMessage) {
    val message: Message? = msg.toMessage()
    when {
        message == null -> sendRspAttachments(daoManager, channel, msg.attachments)
        msg.attachments.isNotEmpty() -> sendRspWithAttachments(daoManager, channel, message, msg.attachments)
        else -> sendRsp(channel, daoManager, message)
    }
}

fun sendRsp(channel: TextChannel, daoManager: DaoManager, message: Message) {
    require(channel.canTalk()) {
        "Cannot talk in this channel: #(${channel.name}, ${channel.id}) - ${channel.guild.id}"
    }

    var action = if (message.contentRaw.isNotBlank()) {
        channel.sendMessage(message.contentRaw)
    } else {
        null
    }

    for (embed in message.embeds) {
        if (action == null) action = channel.sendMessage(embed)
        else action.embed(embed)
    }

    TaskManager.async(channel) {
        val msg = action?.awaitOrNull() ?: return@async

        handleRspDelete(daoManager, msg)
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

suspend fun sendRspAwaitN(textChannel: TextChannel, daoManager: DaoManager, msg: ModularMessage): Message? {
    val message: Message? = msg.toMessage()
    return when {
        message == null -> sendAttachmentsRspAwaitN(textChannel, daoManager, msg.attachments)
        msg.attachments.isNotEmpty() -> sendRspWithAttachmentsAwaitN(textChannel, daoManager, message, msg.attachments)
        else -> sendRspAwaitN(textChannel, daoManager, message)
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

fun sendMsg(textChannel: TextChannel, msg: ModularMessage) {
    val message: Message? = msg.toMessage()
    when {
        message == null -> sendAttachments(textChannel, msg.attachments)
        msg.attachments.isNotEmpty() -> sendMsgWithAttachments(textChannel, message, msg.attachments)
        else -> sendMsg(textChannel, message)
    }
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

suspend fun sendRspAwaitEL(context: CommandContext, msg: String): List<Message> {
    return if (canResponse(context.messageChannel, context.daoManager.supporterWrapper)) {
        sendRspAwaitEL(context.textChannel, context.daoManager, msg)
    } else {
        sendMsgAwaitEL(context, msg)
    }
}

suspend fun sendRspAwaitEL(channel: TextChannel, daoManager: DaoManager, msg: String): List<Message> {
    require(channel.canTalk()) { "Cannot talk in this channel " + channel.name }

    val messageList = mutableListOf<Message>()
    if (msg.length <= 2000) {
        val message = channel.sendMessage(msg).awaitOrNull() ?: return messageList
        messageList.add(message)

        TaskManager.async(channel) {
            handleRspDelete(daoManager, message)
        }

    } else {
        val msgParts = StringUtils.splitMessage(msg).withIndex()
        for ((index, text) in msgParts) {
            val message = channel.sendMessage(text).awaitOrNull() ?: continue
            messageList.add(index, message)

            TaskManager.async(channel) {
                handleRspDelete(daoManager, message)
            }
        }
    }

    return messageList
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

        channel.sendMessage(msg).awaitOrNull()?.let { messageList.add(it) }
    } else {
        val msgParts = StringUtils.splitMessage(msg).withIndex()
        for ((index, text) in msgParts) {

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

    if (msg is DataMessage) {
        action?.allowedMentions(msg.allowedMentions)
    }

    return action?.awaitOrNull()
}

suspend fun sendMsgAwaitN(channel: PrivateChannel, msg: Message): Message? {
    if (channel.user.isBot) return null

    var action = if (msg.contentRaw.isNotBlank()) {
        channel.sendMessage(msg.contentRaw)
    } else null

    for (embed in msg.embeds) {
        if (action == null) action = channel.sendMessage(embed)
        else action.embed(embed)
    }

    if (msg is DataMessage) {
        action?.allowedMentions(msg.allowedMentions)
    }

    return action?.awaitOrNull()
}

suspend fun sendFeatureRequiresPremiumMessage(context: CommandContext, featurePath: String, featureReplaceMap: Map<String, String> = emptyMap()) {
    var feature = context.getTranslation(featurePath)
    for ((key, replacement) in featureReplaceMap) {
        feature = feature.replace("%$key%", replacement)
    }

    val baseMsg = context.getTranslation("message.feature.requires.premium")
        .withVariable("feature", feature)
        .withVariable("prefix", context.usedPrefix)
    sendRsp(context, baseMsg)
}

suspend fun sendFeatureRequiresGuildPremiumMessage(context: CommandContext, featurePath: String, featureReplaceMap: Map<String, String> = emptyMap()) {
    var feature = context.getTranslation(featurePath)
    for ((key, replacement) in featureReplaceMap) {
        feature = feature.replace("%$key%", replacement)
    }

    val baseMsg = context.getTranslation("message.feature.requires.premium.server")
        .withVariable("feature", feature)
        .withVariable("prefix", context.usedPrefix)
    sendRsp(context, baseMsg)
}

fun getNicerUsedPrefix(settings: Settings, prefix: String): String {
    return if (prefix.contains(settings.id.toString()) && USER_MENTION.matches(prefix)) {
        "@${settings.name} "
    } else {
        prefix
    }
}

suspend fun handleRspDelete(daoManager: DaoManager, msgList: MutableList<Message>) {
    val channel = msgList.first().textChannel
    val timeMap = daoManager.removeResponseWrapper.removeResponseCache.get(channel.guild.idLong).await()
    val seconds = timeMap[channel.idLong] ?: return
    delay(seconds * 1000L)

    val msgIds = msgList.map { it1 -> it1.idLong }
    Container.instance.botDeletedMessageIds.addAll(msgIds)

    channel.deleteMessagesByIds(
        msgIds.map { it.toString() }
    ).queue(null, { Container.instance.botDeletedMessageIds.removeAll(msgIds) })

}

suspend fun handleRspDelete(daoManager: DaoManager, message: Message) {
    val timeMap = daoManager.removeResponseWrapper.removeResponseCache.get(message.textChannel.guild.idLong).await()
    val seconds = timeMap[message.textChannel.idLong] ?: timeMap[message.guild.idLong] ?: return

    delay(seconds * 1000L)
    Container.instance.botDeletedMessageIds.add(message.idLong)

    message.delete().queue(null, { Container.instance.botDeletedMessageIds.remove(message.idLong) })
}