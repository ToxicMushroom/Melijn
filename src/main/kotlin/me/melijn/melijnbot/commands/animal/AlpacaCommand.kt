package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext

class AlpacaCommand : AbstractCommand("command.alpaca") {

    init {
        id = 48
        name = "alpaca"
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: CommandContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}