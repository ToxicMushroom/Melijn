package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getCommandsFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendSyntax

class MetricsCommand : AbstractCommand("command.metrics") {

    init {
        id = 79
        name = "metrics"
        children = arrayOf(LimitArg(root), LimitArg(root))
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }
        val cmds = getCommandsFromArgNMessage(context, 0) ?: return

    }

    class LimitArg(root: String) : AbstractCommand("$root.limit") {

        init {
            name = "limit"
            aliases = arrayOf("top")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    class AllArg(root: String) : AbstractCommand("$root.all") {

        init {
            name = "all"
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}