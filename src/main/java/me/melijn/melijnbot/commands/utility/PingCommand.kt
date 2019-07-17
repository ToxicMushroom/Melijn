package me.melijn.melijnbot.commands.utility

import me.duncte123.botcommons.messaging.MessageUtils
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.ICommand
import me.melijn.melijnbot.objects.translation.Translateable


class PingCommand() : ICommand() {

    init {
        id = 1
        name = Translateable("command.ping.name")
        aliases = arrayOf(Translateable("command.ping.alias1"))
        description = Translateable("command.ping.description")
        commandCategory = CommandCategory.UTILITY
    }

    override fun execute(context: CommandContext) {
        val timeStamp1 = System.currentTimeMillis()
        MessageUtils.sendMsg(context, "\uD83C\uDFD3 Pong!\ngatewayPing: ${context.jda.gatewayPing}ms") { message ->
            val timeStamp2 = System.currentTimeMillis()
            context.jda.restPing.queue { restPing ->
                message.editMessage("${message.contentRaw}\nrestPing: ${restPing}ms\nsendMessagePing: ${timeStamp2 - timeStamp1}ms").queue() { editedMessage ->
                    val timeStamp3 = System.currentTimeMillis()
                    editedMessage.editMessage("${editedMessage.contentRaw}\neditMessagePing: ${timeStamp3 - timeStamp2}ms").queue()
                }
            }
        }
    }
}