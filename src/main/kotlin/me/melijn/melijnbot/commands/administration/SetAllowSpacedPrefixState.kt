package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getBooleanFromArgNMessage

class SetAllowSpacedPrefixState : AbstractCommand("command.setallowspacedprefixstate") {

    init {
        id = 174
        name = "setAllowSpacedPrefixState"
        aliases = arrayOf("sasps")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {

            return
        }
        val state = getBooleanFromArgNMessage(context, 0) ?: return
    }
}