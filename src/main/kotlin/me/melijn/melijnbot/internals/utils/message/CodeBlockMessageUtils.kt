package me.melijn.melijnbot.internals.utils.message

import kotlinx.coroutines.delay
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
    val premiumGuild = context.isFromGuild && context.daoManager.supporterWrapper.getGuilds().contains(context.guildId)
    if (premiumGuild) {
        sendRspCodeBlock(context.textChannel, context.authorId, context.daoManager, msg, lang, shouldPaginate)
    } else {
        sendMsgCodeBlock(context, msg, lang, shouldPaginate)
    }
}

fun sendRspCodeBlock(
    textChannel: TextChannel,
    authorId: Long,
    daoManager: DaoManager,
    msg: String,
    lang: String,
    shouldPaginate: Boolean
) {
    if (!textChannel.canTalk()) return
    if (msg.length <= 2000) {
        TaskManager.async(textChannel) {
            val message = textChannel.sendMessage(msg).awaitOrNull() ?: return@async
            val timeMap = daoManager.removeResponseWrapper.getMap(textChannel.guild.idLong)
            val seconds = timeMap[textChannel.idLong] ?: return@async

            delay(seconds * 1000L)
            Container.instance.botDeletedMessageIds.add(message.idLong)

            message.delete().queue(null, { Container.instance.botDeletedMessageIds.remove(message.idLong) })
        }

    } else {
        val parts = StringUtils.splitMessage(msg, maxLength = 2000 - (8 + lang.length) - if (shouldPaginate) 100 else 0)
        sendRspCodeBlocks(textChannel, authorId, daoManager, parts, lang, shouldPaginate)
    }
}

suspend fun sendRspCodeBlocks(
    context: CommandContext,
    parts: List<String>,
    lang: String
) {
    val premiumGuild = context.isFromGuild && context.daoManager.supporterWrapper.getGuilds().contains(context.guildId)
    if (premiumGuild) {
        sendRspCodeBlocks(context.textChannel, context.authorId, context.daoManager, parts, lang, true)
    } else {
        sendMsgCodeBlocks(context.messageChannel, context.authorId, parts, lang, true)
    }
}

fun sendRspCodeBlocks(
    textChannel: TextChannel,
    authorId: Long,
    daoManager: DaoManager,
    parts: List<String>,
    lang: String,
    shouldPaginate: Boolean
) {
    if (shouldPaginate && parts.size > 1) {
        val paginatedParts = parts.mapIndexed { index, s ->
            when {
                index == 0 -> "$s```\nPage ${index + 1}/${parts.size}"
                index + 1 == parts.size -> "```$lang\n$s\nPage ${index + 1}/${parts.size}"
                else -> "```$lang\n$s```\nPage ${index + 1}/${parts.size}"
            }
        }.toMutableList()

        TaskManager.async(textChannel) {
            val message = textChannel.sendMessage(paginatedParts[0]).awaitOrNull() ?: return@async
            registerPaginationMessage(textChannel, authorId, message, paginatedParts, 0)

            val timeMap = daoManager.removeResponseWrapper.getMap(textChannel.guild.idLong)
            val seconds = timeMap[textChannel.idLong] ?: return@async

            delay(seconds * 1000L)
            Container.instance.botDeletedMessageIds.add(message.idLong)

            message.delete().queue(null, { Container.instance.botDeletedMessageIds.remove(message.idLong) })
        }
    } else if (parts.size > 1) {
        TaskManager.async(textChannel) {
            parts.forEachIndexed { index, msgPart ->
                val message = textChannel.sendMessage(
                    when {
                        index == 0 -> "$msgPart```"
                        index + 1 == parts.size -> "```$lang\n$msgPart"
                        else -> "```$lang\n$msgPart```"
                    }
                ).awaitOrNull() ?: return@async

                launch {
                    val timeMap = daoManager.removeResponseWrapper.getMap(textChannel.guild.idLong)
                    val seconds = timeMap[textChannel.idLong] ?: return@launch

                    delay(seconds * 1000L)
                    Container.instance.botDeletedMessageIds.add(message.idLong)

                    message.delete().queue(null, { Container.instance.botDeletedMessageIds.remove(message.idLong) })
                }
            }
        }

    } else {
        TaskManager.async(textChannel) {
            val message = textChannel.sendMessage(parts[0]).awaitOrNull() ?: return@async

            val timeMap = daoManager.removeResponseWrapper.getMap(textChannel.guild.idLong)
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
            val parts =
                StringUtils.splitMessage(msg, maxLength = 2000 - (8 + lang.length) - if (shouldPaginate) 100 else 0)
            sendMsgCodeBlocks(channel, context.authorId, parts, lang, shouldPaginate)
        }

    } else {

        val privateChannel = context.privateChannel
        if (msg.length <= 2000) {
            privateChannel.sendMessage(msg).queue()
        } else {
            val parts = StringUtils.splitMessage(msg, maxLength = 2000 - (8 + lang.length))
            sendMsgCodeBlocks(privateChannel, context.authorId, parts, lang, shouldPaginate)
        }
    }
}

suspend fun sendMsgCodeBlocks(
    messageChannel: MessageChannel,
    authorId: Long,
    parts: List<String>,
    lang: String,
    shouldPaginate: Boolean
) {
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
                registerPaginationMessage(messageChannel, authorId, it, paginatedParts, 0)
            } else if (messageChannel is PrivateChannel) {
                registerPaginationMessage(messageChannel, authorId, it, paginatedParts, 0)
            }
        }
    } else {
        parts.forEachIndexed { index, msgPart ->
            messageChannel.sendMessage(
                when {
                    index == 0 -> "$msgPart```"
                    index + 1 == parts.size -> "```$lang\n$msgPart"
                    else -> "```$lang\n$msgPart```"
                }
            ).queue()
        }
    }
}