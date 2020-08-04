package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.message.handleRspDelete
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.withVariable


class PingCommand : AbstractCommand("command.ping") {

    init {
        id = 1
        name = "ping"
        aliases = arrayOf("pong", "latency")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val timeStamp1 = System.currentTimeMillis()

        val part1 = context.getTranslation("$root.response1.part1")
            .withVariable("gatewayPing", context.jda.gatewayPing.toString())

        val part2 = context.getTranslation("$root.response1.part2")
        val part3 = context.getTranslation("$root.response1.part3")

        val message = sendMsgAwaitEL(context, part1)
        val timeStamp2 = System.currentTimeMillis()
        val msgPing = timeStamp2 - timeStamp1
        val restPing = context.jda.restPing.await()

        val editedMessage = message[0].editMessage("${message[0].contentRaw}${replacePart2(part2, restPing, msgPing)}").await()
        val timeStamp3 = System.currentTimeMillis()
        val eMsgPing = timeStamp3 - timeStamp2

        editedMessage.editMessage("${editedMessage.contentRaw}${replacePart3(part3, eMsgPing)}").queue { c ->
            TaskManager.async(context) {
                handleRspDelete(context.daoManager, c)
            }
        }
    }


    private fun replacePart2(string: String, restPing: Long, sendMessagePing: Long): String = string
        .withVariable("restPing", "$restPing")
        .withVariable("sendMessagePing", "$sendMessagePing")


    private fun replacePart3(string: String, editMessagePing: Long): String = string
        .withVariable("editMessagePing", "$editMessagePing")

}