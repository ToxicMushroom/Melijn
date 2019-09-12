package me.melijn.melijnbot.commandutil.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.message.MessageWrapper
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.getColorFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.toHex

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

            modularMessage.attachments = newMap.toMap()

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

            modularMessage.attachments = attachments.toMap()

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

        suspend fun showEmbedColor(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val oldMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val color = oldMessage?.embed?.color

            val msg = if (color == null) {
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                Translateable("${cmd.root}.show.set").string(context)
                    .replace("%color%", color.toHex())
            }

            sendMsg(context, msg)
        }

        suspend fun setEmbedColor(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                ?: ModularMessage()

            val arg = context.args[0]

            val msg = if (arg.equals("null", true)) {
                messageWrapper.removeEmbedColor(modularMessage, context.getGuildId(), type)
                Translateable("${cmd.root}.unset").string(context)
            } else {
                val color = getColorFromArgNMessage(context, 0) ?: return
                messageWrapper.setEmbedColor(modularMessage, context.getGuildId(), type, color)
                Translateable("${cmd.root}.set").string(context)
                    .replace("%newColor%", color.toHex())
            }

            sendMsg(context, msg)
        }

        suspend fun setEmbedTitle(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                ?: ModularMessage()

            val arg = context.args[0]

            val msg = if (arg.equals("null", true)) {
                messageWrapper.removeEmbedTitleContent(modularMessage, context.getGuildId(), type)
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                messageWrapper.setEmbedTitleContent(modularMessage, context.getGuildId(), type, context.rawArg)
                Translateable("${cmd.root}.show.set").string(context)
                    .replace(PLACEHOLDER_ARG, context.rawArg)
            }

            sendMsg(context, msg)
        }

        suspend fun showEmbedTitle(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val title = modularMessage?.embed?.title

            val msg = if (title == null) {
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                Translateable("${cmd.root}.show.set").string(context)
                    .replace("%title%", title)
            }

            sendMsg(context, msg)
        }

        suspend fun showEmbedTitleUrl(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val url = modularMessage?.embed?.url

            val msg = if (url == null) {
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                Translateable("${cmd.root}.show.set").string(context)
                    .replace("%url%", url)
            }

            sendMsg(context, msg)
        }

        suspend fun setEmbedTitleUrl(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                ?: ModularMessage()

            val arg = context.args[0]

            val msg = if (arg.equals("null", true)) {
                messageWrapper.removeEmbedTitleURL(modularMessage, context.getGuildId(), type)
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                messageWrapper.setEmbedTitleURL(modularMessage, context.getGuildId(), type, context.rawArg)
                Translateable("${cmd.root}.show.set").string(context)
                    .replace(PLACEHOLDER_ARG, context.rawArg)
            }

            sendMsg(context, msg)
        }

        suspend fun showEmbedAuthor(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val url = modularMessage?.embed?.author?.name

            val msg = if (url == null) {
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                Translateable("${cmd.root}.show.set").string(context)
                    .replace("%name%", url)
            }

            sendMsg(context, msg)
        }

        suspend fun setEmbedAuthor(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                ?: ModularMessage()

            val arg = context.args[0]

            val msg = if (arg.equals("null", true)) {
                messageWrapper.removeEmbedAuthorContent(modularMessage, context.getGuildId(), type)
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                messageWrapper.setEmbedAuthorContent(modularMessage, context.getGuildId(), type, context.rawArg)
                Translateable("${cmd.root}.show.set").string(context)
                    .replace(PLACEHOLDER_ARG, context.rawArg)
            }

            sendMsg(context, msg)
        }

        suspend fun showEmbedAuthorIcon(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val url = modularMessage?.embed?.author?.iconUrl

            val msg = if (url == null) {
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                Translateable("${cmd.root}.show.set").string(context)
                    .replace("%url", url)
            }

            sendMsg(context, msg)
        }

        suspend fun setEmbedAuthorIcon(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                ?: ModularMessage()

            val arg = context.args[0]

            val msg = if (arg.equals("null", true)) {
                messageWrapper.removeEmbedAuthorIconURL(modularMessage, context.getGuildId(), type)
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                messageWrapper.setEmbedAuthorIconURL(modularMessage, context.getGuildId(), type, context.rawArg)
                Translateable("${cmd.root}.show.set").string(context)
                    .replace(PLACEHOLDER_ARG, context.rawArg)
            }

            sendMsg(context, msg)
        }

        suspend fun showEmbedAuthorUrl(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val url = modularMessage?.embed?.author?.url

            val msg = if (url == null) {
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                Translateable("${cmd.root}.show.set").string(context)
                    .replace("%url", url)
            }

            sendMsg(context, msg)
        }

        suspend fun setEmbedAuthorUrl(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                ?: ModularMessage()

            val arg = context.args[0]

            val msg = if (arg.equals("null", true)) {
                messageWrapper.removeEmbedAuthorURL(modularMessage, context.getGuildId(), type)
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                messageWrapper.setEmbedAuthorURL(modularMessage, context.getGuildId(), type, context.rawArg)
                Translateable("${cmd.root}.show.set").string(context)
                    .replace(PLACEHOLDER_ARG, context.rawArg)
            }

            sendMsg(context, msg)
        }

        suspend fun showEmbedThumbnail(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val url = modularMessage?.embed?.thumbnail?.url

            val msg = if (url == null) {
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                Translateable("${cmd.root}.show.set").string(context)
                    .replace("%url", url)
            }

            sendMsg(context, msg)
        }

        suspend fun setEmbedThumbnail(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                ?: ModularMessage()

            val arg = context.args[0]

            val msg = if (arg.equals("null", true)) {
                messageWrapper.removeEmbedThumbnail(modularMessage, context.getGuildId(), type)
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                messageWrapper.setEmbedThumbnail(modularMessage, context.getGuildId(), type, context.rawArg)
                Translateable("${cmd.root}.show.set").string(context)
                    .replace(PLACEHOLDER_ARG, context.rawArg)
            }

            sendMsg(context, msg)
        }

        suspend fun showEmbedImage(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val url = modularMessage?.embed?.image?.url

            val msg = if (url == null) {
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                Translateable("${cmd.root}.show.set").string(context)
                    .replace("%url", url)
            }

            sendMsg(context, msg)
        }

        suspend fun setEmbedImage(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                ?: ModularMessage()

            val arg = context.args[0]

            val msg = if (arg.equals("null", true)) {
                messageWrapper.removeEmbedImage(modularMessage, context.getGuildId(), type)
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                messageWrapper.setEmbedImage(modularMessage, context.getGuildId(), type, context.rawArg)
                Translateable("${cmd.root}.show.set").string(context)
                    .replace(PLACEHOLDER_ARG, context.rawArg)
            }

            sendMsg(context, msg)
        }

        suspend fun showEmbedFooter(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val footer = modularMessage?.embed?.footer?.text

            val msg = if (footer == null) {
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                Translateable("${cmd.root}.show.set").string(context)
                    .replace("%url", footer)
            }

            sendMsg(context, msg)
        }

        suspend fun setEmbedFooter(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                ?: ModularMessage()

            val arg = context.args[0]

            val msg = if (arg.equals("null", true)) {
                messageWrapper.removeEmbedFooterContent(modularMessage, context.getGuildId(), type)
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                messageWrapper.setEmbedFooterContent(modularMessage, context.getGuildId(), type, context.rawArg)
                Translateable("${cmd.root}.show.set").string(context)
                    .replace(PLACEHOLDER_ARG, context.rawArg)
            }

            sendMsg(context, msg)
        }

        fun showEmbedFooterUrl(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        suspend fun setEmbedFooterUrl(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
                ?: ModularMessage()

            val arg = context.args[0]

            val msg = if (arg.equals("null", true)) {
                messageWrapper.removeEmbedFooterURL(modularMessage, context.getGuildId(), type)
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                messageWrapper.setEmbedFooterURL(modularMessage, context.getGuildId(), type, context.rawArg)
                Translateable("${cmd.root}.show.set").string(context)
                    .replace(PLACEHOLDER_ARG, context.rawArg)
            }

            sendMsg(context, msg)
        }
    }
}