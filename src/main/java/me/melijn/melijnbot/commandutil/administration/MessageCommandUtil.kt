package me.melijn.melijnbot.commandutil.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.sendMsg

class MessageCommandUtil {
    companion object {
        suspend fun setMessageContent(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val oldMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val newContent = context.rawArg


            val msg = if (newContent.equals("null", true)) {
                messageWrapper.removeMessageContent(oldMessage, context.getGuildId(), type)
                Translateable("${cmd.root}.set.unset").string(context)
            } else {
                messageWrapper.setMessageContent(oldMessage, context.getGuildId(), type, newContent)
                Translateable("${cmd.root}.set").string(context)
                        .replace("%newContent%", newContent)
            }.replace("%oldContent%", oldMessage?.messageContent ?: "/")

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
                for ((index, attachment) in modularMessage.attachments.withIndex()) {
                    content += "\n$index - [$attachment]"
                }
                content += "```"
                (title + content)
            }
            sendMsg(context, msg)


        }

        fun addAttachment(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        fun removeAttachment(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        fun showEmbedColor(cmd: AbstractCommand, context: CommandContext, type: MessageType) {

        }

        fun setEmbedColor(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}