package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.message.handleRspDelete
import me.melijn.melijnbot.internals.utils.message.sendEmbedAwaitEL
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.Permission

class PingCommand : AbstractCommand("command.ping") {

    init {
        id = 1
        name = "ping"
        aliases = arrayOf("pong", "latency")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val part1 = context.getTranslation("$root.response1.part1")
            .withVariable("gatewayPing", context.jda.gatewayPing)

        val part2 = context.getTranslation("$root.response1.part2")
        val part3 = context.getTranslation("$root.response1.part3")

        val eb = Embedder(context)
            .setTitle("\uD83C\uDFD3 Ping!")

        val timeStamp1 = System.currentTimeMillis()
        eb.setDescription(part1)
        val message = sendEmbedAwaitEL(context, eb.build())
        if (message.isEmpty()) return

        val timeStamp2 = System.currentTimeMillis()

        val msgPing = timeStamp2 - timeStamp1
        val restPing = context.jda.restPing.await()

        val timeStamp3 = System.currentTimeMillis()
        eb.appendDescription(replacePart2(part2, restPing, msgPing))
        val editedMessage = message[0].editMessageEmbeds(eb.build()).await()
        val timeStamp4 = System.currentTimeMillis()
        val eMsgPing = timeStamp4 - timeStamp3

        eb.appendDescription(replacePart3(part3, eMsgPing))
        editedMessage.editMessageEmbeds(eb.build()).queue { c ->
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