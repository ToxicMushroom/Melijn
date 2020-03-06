package me.melijn.melijnbot.commands.developer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendPaginationMsg


class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        sendPagination(context)
    }

    private suspend fun sendPagination(context: CommandContext) {
        val list = mutableListOf(
            "aaa",
            "bbb",
            "ccc"
        )

        sendPaginationMsg(context, list, 0)
    }

    private suspend fun ffmPegFlushed(context: CommandContext) = withContext(Dispatchers.IO) {

    }


}