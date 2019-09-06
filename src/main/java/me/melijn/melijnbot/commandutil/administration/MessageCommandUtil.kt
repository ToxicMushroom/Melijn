package me.melijn.melijnbot.commandutil.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.message.MessageWrapper
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.sendMsg

class MessageCommandUtil {
    companion object {
        fun removeMessageIfEmpty(guildId: Long, type: MessageType, message: ModularMessage, messageWrapper: MessageWrapper): Boolean {
            return if (messageWrapper.shouldRemove(message)) {
                messageWrapper.removeMessage(guildId, type)
                true
            } else false
        }

        suspend fun setMessageContent(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val oldMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                    ?: ModularMessage()
            val newContent = context.rawArg


            val msg = if (newContent.equals("null", true)) {
                messageWrapper.removeMessageContent(oldMessage, context.getGuildId(), type)
                Translateable("${cmd.root}.set.unset").string(context)
            } else {
                messageWrapper.setMessageContent(oldMessage, context.getGuildId(), type, newContent)
                Translateable("${cmd.root}.set").string(context)
                        .replace("%newContent%", newContent)
            }.replace("%oldContent%", oldMessage.messageContent ?: "/")

            sendMsg(context, msg)
        }

        suspend fun showMessageContent(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val content = modularMessage?.messageContent
            val msg = if (content == null) {
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                Translateable("${cmd.root}.show.set").string(context)
                        .replace("%content%", content)
            }

            sendMsg(context, msg)
        }

        suspend fun setEmbedDescription(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val oldMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val arg = context.rawArg


            val msg = if (arg.equals("null", true)) {
                if (oldMessage != null) {
                    messageWrapper.removeEmbedDescription(oldMessage, context.getGuildId(), type)
                }
                Translateable("${cmd.root}.set.unset").string(context)
            } else {
                messageWrapper.setEmbedDescription(oldMessage ?: ModularMessage(), context.getGuildId(), type, arg)
                Translateable("${cmd.root}.set").string(context)
                        .replace("%newContent%", arg)
            }.replace("%oldContent%", oldMessage?.embed?.description ?: "/")

            sendMsg(context, msg)
        }

        suspend fun showEmbedDescription(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val description = modularMessage?.embed?.description
            val msg = if (description == null) {
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                Translateable("${cmd.root}.show.set").string(context)
                        .replace("%content%", description)
            }

            sendMsg(context, msg)
        }

        suspend fun clearEmbed(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()

            if (modularMessage != null) {
                messageWrapper.clearEmbed(modularMessage, context.getGuildId(), type)
            }

            val msg = Translateable("${cmd.root}.success")
                    .string(context)

            sendMsg(context, msg)

        }

        suspend fun listAttachments(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()

            val msg = if (modularMessage == null || modularMessage.attachments.isEmpty()) {
                Translateable("${cmd.root}.empty").string(context)

            } else {
                val title = Translateable("${cmd.root}.title").string(context)
                var content = "\n```INI"
                for ((index, attachment) in modularMessage.attachments.entries.withIndex()) {
                    content += "\n$index - [${attachment.key}] - ${attachment.value}"
                }
                content += "```"
                (title + content)
            }
            sendMsg(context, msg)
        }

        suspend fun addAttachment(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                    ?: ModularMessage()

            val newMap = modularMessage.attachments.toMutableMap()
            newMap[context.args[0]] = context.args[1]

            messageWrapper.setMessage(context.getGuildId(), type, modularMessage)
            val msg = Translateable("${cmd.root}.success").string(context)
                    .replace("%attachment%", context.args[0])
                    .replace("%file%", context.args[1])

            sendMsg(context, msg)
        }

        suspend fun removeAttachment(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                    ?: ModularMessage()

            val attachments = modularMessage.attachments.toMutableMap()
            val file = if (attachments.containsKey(context.args[0])) attachments[context.args[0]] else null
            attachments.remove(context.args[0])

            val msg =
                    if (file == null) {
                        Translateable("${cmd.root}.notanattachment").string(context)
                                .replace("%attachment%", context.args[0])
                    } else {
                        messageWrapper.setMessage(context.getGuildId(), type, modularMessage)
                        Translateable("${cmd.root}.success").string(context)
                                .replace("%attachment%", context.args[0])
                                .replace("%file%", file)
                    }

            sendMsg(context, msg)
        }

        fun showEmbedColor(cmd: AbstractCommand, context: CommandContext, type: MessageType) {

        }

        fun setEmbedColor(cmd: AbstractCommand, context: CommandContext, type: MessageType) {

        }
    }
}