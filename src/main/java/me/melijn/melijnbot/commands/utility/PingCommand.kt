package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.await
import me.melijn.melijnbot.objects.utils.sendMsg


class PingCommand : AbstractCommand("command.ping") {

    init {
        id = 1
        name = "ping"
        syntax = Translateable("$root.syntax")
        aliases = arrayOf("pong")
        description = Translateable("$root.description")
        commandCategory = CommandCategory.UTILITY
        children = arrayOf(PongCommand())
    }

    override suspend fun execute(context: CommandContext) {
        val timeStamp1 = System.currentTimeMillis()
        val part1 = replaceGatewayPing(
                Translateable("command.ping.response1.part1").string(context),
                context.jda.gatewayPing
        )
        val part2 = Translateable("command.ping.response1.part2").string(context)
        val part3 = Translateable("command.ping.response1.part3").string(context)

        val message = sendMsg(context, part1)
        val timeStamp2 = System.currentTimeMillis()
        val msgPing = timeStamp2 - timeStamp1
        val restPing = context.jda.restPing.await()

        val editedMessage = message.editMessage("${message.contentRaw}${replacePart2(part2, restPing, msgPing)}").await()
        val timeStamp3 = System.currentTimeMillis()
        val eMsgPing = timeStamp3 - timeStamp2
        editedMessage.editMessage("${editedMessage.contentRaw}${replacePart3(part3, eMsgPing)}").queue()

    }

    private fun replaceGatewayPing(string: String, gatewayPing: Long): String = string
            .replace("%gatewayPing%", "$gatewayPing")


    private fun replacePart2(string: String, restPing: Long, sendMessagePing: Long): String = string
            .replace("%restPing%", "$restPing")
            .replace("%sendMessagePing%", "$sendMessagePing")


    private fun replacePart3(string: String, editMessagePing: Long): String = string
            .replace("%editMessagePing%", "$editMessagePing")


    private class PongCommand : AbstractCommand("command.ping.pong") {

        init {
            name = "pong"
            aliases = arrayOf("ping")
            children = arrayOf(DunsteCommand())
        }

        override suspend fun execute(context: CommandContext) {
            sendMsg(context, Translateable("$root.response1").string(context))
        }

        private class DunsteCommand : AbstractCommand("command.ping.pong.dunste") {

            init {
                name = "dunste"
                aliases = arrayOf("duncte")
            }

            override suspend fun execute(context: CommandContext) {
                sendMsg(context, Translateable("$root.response1").string(context))
            }
        }
    }
}