package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.await
import me.melijn.melijnbot.objects.utils.sendMsg


class PingCommand : AbstractCommand("command.ping") {

    init {
        id = 1
        name = "ping"
        aliases = arrayOf("pong")
        commandCategory = CommandCategory.UTILITY
        children = arrayOf(PongCommand())
    }

    override suspend fun execute(context: CommandContext) {
        val timeStamp1 = System.currentTimeMillis()
        val language = context.getLanguage()
        val part1 = replaceGatewayPing(
            i18n.getTranslation(language, "command.ping.response1.part1"),
            context.jda.gatewayPing
        )
        val part2 = i18n.getTranslation(language, "command.ping.response1.part2")
        val part3 = i18n.getTranslation(language, "command.ping.response1.part3")

        val message = sendMsg(context, part1)
        val timeStamp2 = System.currentTimeMillis()
        val msgPing = timeStamp2 - timeStamp1
        val restPing = context.jda.restPing.await()

        val editedMessage = message[0].editMessage("${message[0].contentRaw}${replacePart2(part2, restPing, msgPing)}").await()
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
            val language = context.getLanguage()
            sendMsg(context, i18n.getTranslation(language, "$root.response1"))
        }

        private class DunsteCommand : AbstractCommand("command.ping.pong.dunste") {

            init {
                name = "dunste"
                aliases = arrayOf("duncte")
            }

            override suspend fun execute(context: CommandContext) {
                val language = context.getLanguage()
                sendMsg(context, i18n.getTranslation(language, "$root.response1"))
            }
        }
    }
}