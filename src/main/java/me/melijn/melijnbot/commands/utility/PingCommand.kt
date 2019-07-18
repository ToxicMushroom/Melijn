package me.melijn.melijnbot.commands.utility

import me.duncte123.botcommons.messaging.MessageUtils
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PREFIX_PLACE_HOLDER
import me.melijn.melijnbot.objects.translation.Translateable


class PingCommand : AbstractCommand() {

    init {
        id = 1
        name = "ping"
        syntax = "$PREFIX_PLACE_HOLDER$name [pong] [dunste]"
        aliases = arrayOf("pong")
        description = Translateable("command.ping.description")
        commandCategory = CommandCategory.UTILITY
        children = arrayOf(PongCommand())
    }

    override fun execute(context: CommandContext) {
        val timeStamp1 = System.currentTimeMillis()
        val part1 = replaceGatewayPing(Translateable("command.ping.response1.part1").string(context), context.jda.gatewayPing)
        val part2 = Translateable("command.ping.response1.part2").string(context)
        val part3 = Translateable("command.ping.response1.part3").string(context)
        MessageUtils.sendMsg(context, part1) { message ->
            val timeStamp2 = System.currentTimeMillis()
            val msgPing = timeStamp2 - timeStamp1
            context.jda.restPing.queue { restPing ->
                message.editMessage("${message.contentRaw}${replacePart2(part2, restPing, msgPing)}").queue() { editedMessage ->
                    val timeStamp3 = System.currentTimeMillis()
                    val eMsgPing = timeStamp3 - timeStamp2
                    editedMessage.editMessage("${editedMessage.contentRaw}${replacePart3(part3, eMsgPing)}").queue()
                }
            }
        }
    }

    fun replaceGatewayPing(string: String, gatewayPing: Long): String {
        return string
                .replace("%gatewayPing%", "$gatewayPing")
    }

    fun replacePart2(string: String, restPing: Long, sendMessagePing: Long): String {
        return string
                .replace("%restPing%", "$restPing")
                .replace("%sendMessagePing%", "$sendMessagePing")
    }

    fun replacePart3(string: String, editMessagePing: Long): String {
        return string
                .replace("%editMessagePing%", "$editMessagePing")
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