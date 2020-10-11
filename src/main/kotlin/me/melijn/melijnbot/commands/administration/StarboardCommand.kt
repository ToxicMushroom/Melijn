package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext

class StarboardCommand : AbstractCommand("command.starboard") {
    init {
        id=224
        name="starboard"
        commandCategory=CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {

    }
}