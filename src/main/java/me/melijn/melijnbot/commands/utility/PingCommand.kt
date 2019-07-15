package me.melijn.melijnbot.commands.utility

import me.duncte123.botcommons.messaging.MessageUtils
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.ICommand

class PingCommand() : ICommand() {

    init {
        id = 1
        name = "ping"
        aliases = arrayOf("pong")
        description = "Show network latency to discord servers"
        commandCategory = CommandCategory.UTILITY
    }

    override fun execute(context: CommandContext) {
        val timeStamp1 = System.currentTimeMillis()
        MessageUtils.sendMsg(context, "\uD83C\uDFD3 Pong! gatewayPing: ${context.jda.gatewayPing}ms") { message ->
            val timeStamp2 = System.currentTimeMillis()
            context.jda.restPing.queue { restPing ->
                message.editMessage("${message.contentRaw}, restPing: ${restPing}ms, sendMessagePing: ${timeStamp2 - timeStamp1}ms").queue() { editedMessage ->
                    val timeStamp3 = System.currentTimeMillis()
                    editedMessage.editMessage("${message.contentRaw}, editMessagePing: ${timeStamp3 - timeStamp2}ms").queue()
                }
            }
        }

    }
}