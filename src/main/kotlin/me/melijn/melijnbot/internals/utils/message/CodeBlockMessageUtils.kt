package me.melijn.melijnbot.internals.utils.message

import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.StringUtils
import me.melijn.melijnbot.internals.utils.awaitOrNull
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.entities.TextChannel


suspend fun sendRspCodeBlock(context: CommandContext, msg: String, lang: String, shouldPaginate: Boolean = false) {
    val premiumGuild = context.isFromGuild && context.daoManager.supporterWrapper.guildSupporterIds.contains(context.guildId)
    if (premiumGuild) {
        sendRspCodeBlock(context.textChannel, context.daoManager, context.taskManager, msg, lang, shouldPaginate)
    } else {
        sendMsgCodeBlock(context, msg, lang, shouldPaginate)
    }
}

fun sendRspCodeBlock(textChannel: TextChannel, daoManager: DaoManager, taskManager: TaskManager, msg: String, lang: String, shouldPaginate: Boolean) {
    if (!textChannel.canTalk()) return
    if (msg.length <= 2000) {
        taskManager.async {
            val message = textChannel.sendMessage(msg).awaitOrNull() ?: return@async
            val timeMap = daoManager.removeResponseWrapper.removeResponseCache.get(textChannel.guild.idLong).await()
            val seconds = timeMap[textChannel.idLong] ?: return@async

            delay(seconds * 1000L)
            Container.instance.botDeletedMessageIds.add(message.idLong)

            message.delete().queue(null, { Container.instance.botDeletedMessageIds.remove(message.idLong) })
        }

    } else {
        val parts = StringUtils.splitMessage(msg, maxLength = 2000 - (8 + lang.length) - if (shouldPaginate) 100 else 0)
        sendRspCodeBlocks(textChannel, daoManager, taskManager, parts, lang, shouldPaginate)
    }
}

suspend fun sendRspCodeBlocks(
    context: CommandContext,
    parts: List<String>,
    lang: String
) {
    val premiumGuild = context.isFromGuild && context.daoManager.supporterWrapper.guildSupporterIds.contains(context.guildId)
    if (premiumGuild) {
        sendRspCodeBlocks(context.textChannel, context.daoManager, context.taskManager, parts, lang, true)
    } else {
        sendMsgCodeBlocks(context.messageChannel, parts, lang, true)
    }
}

fun sendRspCodeBlocks(textChannel: TextChannel, daoManager: DaoManager, taskManager: TaskManager, parts: List<String>, lang: String, shouldPaginate: Boolean) {
    if (shouldPaginate && parts.size > 1) {
        val paginatedParts = parts.mapIndexed { index, s ->
            when {
                index == 0 -> "$s```\nPage ${index + 1}/${parts.size}"
                index + 1 == parts.size -> "```$lang\n$s\nPage ${index + 1}/${parts.size}"
                else -> "```$lang\n$s```\nPage ${index + 1}/${parts.size}"
            }
        }.toMutableList()

        taskManager.async {
            val message = textChannel.sendMessage(paginatedParts[0]).awaitOrNull() ?: return@async
            registerPaginationMessage(textChannel, message, paginatedParts, 0)

            val timeMap = daoManager.removeResponseWrapper.removeResponseCache.get(textChannel.guild.idLong).await()
            val seconds = timeMap[textChannel.idLong] ?: return@async

            delay(seconds * 1000L)
            Container.instance.botDeletedMessageIds.add(message.idLong)

            message.delete().queue(null, { Container.instance.botDeletedMessageIds.remove(message.idLong) })
        }
    } else if (parts.size > 1) {
        taskManager.async {
            parts.forEachIndexed { index, msgPart ->
                val message = textChannel.sendMessage(when {
                    index == 0 -> "$msgPart```"
                    index + 1 == parts.size -> "```$lang\n$msgPart"
                    else -> "```$lang\n$msgPart```"
                }).awaitOrNull() ?: return@async

                launch {
                    val timeMap = daoManager.removeResponseWrapper.removeResponseCache.get(textChannel.guild.idLong).await()
                    val seconds = timeMap[textChannel.idLong] ?: return@launch

                    delay(seconds * 1000L)
                    Container.instance.botDeletedMessageIds.add(message.idLong)

                    message.delete().queue(null, { Container.instance.botDeletedMessageIds.remove(message.idLong) })
                }
            }
        }

    } else {
        taskManager.async {
            val message = textChannel.sendMessage(parts[0]).awaitOrNull() ?: return@async

            val timeMap = daoManager.removeResponseWrapper.removeResponseCache.get(textChannel.guild.idLong).await()
            val seconds = timeMap[textChannel.idLong] ?: return@async

            delay(seconds * 1000L)
            Container.instance.botDeletedMessageIds.add(message.idLong)

            message.delete().queue(null, { Container.instance.botDeletedMessageIds.remove(message.idLong) })
        }
    }
}

suspend fun sendMsgCodeBlock(context: CommandContext, msg: String, lang: String, shouldPaginate: Boolean = false) {
    if (context.isFromGuild) {
        val channel = context.textChannel
        if (!channel.canTalk()) return
        if (msg.length <= 2000) {
            channel.sendMessage(msg).queue()
        } else {
            val parts = StringUtils.splitMessage(msg, maxLength = 2000 - (8 + lang.length) - if (shouldPaginate) 100 else 0)
            sendMsgCodeBlocks(channel, parts, lang, shouldPaginate)
        }

    } else {

        val privateChannel = context.privateChannel
        if (msg.length <= 2000) {
            privateChannel.sendMessage(msg).queue()
        } else {
            val parts = StringUtils.splitMessage(msg, maxLength = 2000 - (8 + lang.length))
            sendMsgCodeBlocks(privateChannel, parts, lang, shouldPaginate)
        }
    }
}

suspend fun sendMsgCodeBlocks(messageChannel: MessageChannel, parts: List<String>, lang: String, shouldPaginate: Boolean) {
    if (shouldPaginate && parts.size > 1) {
        val paginatedParts = parts.mapIndexed { index, s ->
            when {
                index == 0 -> "$s```\nPage ${index + 1}/${parts.size}"
                index + 1 == parts.size -> "```$lang\n$s\nPage ${index + 1}/${parts.size}"
                else -> "```$lang\n$s```\nPage ${index + 1}/${parts.size}"
            }
        }.toMutableList()

        val message = messageChannel.sendMessage(paginatedParts[0]).awaitOrNull()
        message?.let {
            if (messageChannel is TextChannel) {
                registerPaginationMessage(messageChannel, it, paginatedParts, 0)
            } else if (messageChannel is PrivateChannel) {
                registerPaginationMessage(messageChannel, it, paginatedParts, 0)
            }
        }
    } else {
        parts.forEachIndexed { index, msgPart ->
            messageChannel.sendMessage(when {
                index == 0 -> "$msgPart```"
                index + 1 == parts.size -> "```$lang\n$msgPart"
                else -> "```$lang\n$msgPart```"
            }).queue()
        }
    }
}