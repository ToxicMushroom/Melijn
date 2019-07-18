package me.melijn.melijnbot.commands.utility

import me.duncte123.botcommons.messaging.MessageUtils
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable


class PingCommand : AbstractCommand() {

    init {
        id = 1
        name = "ping"//Translateable("command.ping.name")
        syntax = Translateable("command.ping.syntax")
        aliases = arrayOf("pong")//Translateable("command.ping.alias1"))
        description = Translateable("command.ping.description")
        commandCategory = CommandCategory.UTILITY
        children = arrayOf(PongCommand())
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

    private class PongCommand : AbstractCommand() {

        init {
            name = "pong"//Translateable("command.ping.pong.name")
            aliases = arrayOf("ping")//Translateable("command.ping.pong.alias1"))
            children = arrayOf(DunsteCommand())
        }

        override fun execute(context: CommandContext) {
            MessageUtils.sendMsg(context, Translateable("command.ping.pong.response1").string(context))
        }

        private class DunsteCommand : AbstractCommand() {

            init {
                name = "dunste"
                aliases = arrayOf("duncte")
            }

            override fun execute(context: CommandContext) {
                MessageUtils.sendMsg(context, Translateable("command.ping.pong.dunste.response1").string(context))
            }
        }
    }
}