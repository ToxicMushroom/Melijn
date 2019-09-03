package me.melijn.melijnbot.commandutil.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.sendMsg

class MessageCommandUtil{
    companion object {
        suspend fun setMessageContent(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val oldMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val newContent = context.rawArg


            val msg = if (newContent.equals("null", true)) {
                messageWrapper.removeMessage(context.getGuildId(), type)
                Translateable("${cmd.root}.set.unset").string(context)
                        .replace("%oldContent%", oldContent)
            } else {
                messageWrapper.setMessage(context.getGuildId(), type, newContent)
                Translateable("${cmd.root}.set").string(context)
                        .replace("%oldContent%", oldContent)
                        .replace("%newContent%", newContent)
            }

            sendMsg(context, msg)
        }

        suspend fun showMessageContent(cmd: AbstractCommand, context: CommandContext, type: MessageType) {
            val messageWrapper = context.daoManager.messageWrapper
            val content = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            val msg = if (content.isBlank()) {
                Translateable("${cmd.root}.show.unset").string(context)
            } else {
                Translateable("${cmd.root}.show.set").string(context)
                        .replace("%content%", content)
            }

            sendMsg(context, msg)
        }
    }
}