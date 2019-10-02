package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendSyntax

class PurgeCommand : AbstractCommand("command.purge") {

    init {
        id = 39
        name = "purge"
        aliases = arrayOf("spurge")
        commandCategory = CommandCategory.MODERATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }

        val amount = getIntegerFromArgNMessage(context, 0, 1, 1000)
    }
}