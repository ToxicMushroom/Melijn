package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.getCommandsFromArgNMessage
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.getTimespanFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class MetricsCommand : AbstractCommand("command.metrics") {

    init {
        id = 79
        name = "metrics"
        children = arrayOf(
            LimitArg(root),
            AllArg(root)
        )
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val cmdList = getCommandsFromArgNMessage(context, 0)?.map { cmd -> cmd.id } ?: return
        val timespan: Pair<Long, Long> = getTimespanFromArgNMessage(context, 1)
        val wrapper = context.daoManager.commandUsageWrapper
        val result = wrapper.getFilteredUsageWithinPeriod(timespan.first, timespan.second, cmdList)

        var msg = "```INI"
        for ((cmd, usageCount) in result) {
            msg += "\n$usageCount - [${cmd.name}]"
        }
        msg += "```"

        sendRsp(context, msg)
    }

    class LimitArg(root: String) : AbstractCommand("$root.limit") {

        init {
            name = "limit"
            aliases = arrayOf("top")
        }

        override suspend fun execute(context: CommandContext) {
            val limit = getIntegerFromArgNMessage(context, 0, 0, Container.instance.commandMap.size) ?: return
            val timespan: Pair<Long, Long> = getTimespanFromArgNMessage(context, 1)
            val wrapper = context.daoManager.commandUsageWrapper
            val result = wrapper.getTopUsageWithinPeriod(timespan.first, timespan.second, limit)

            var msg = "```INI"
            for ((cmd, usageCount) in result) {
                msg += "\n$usageCount - [${cmd.name}]"
            }
            msg += "```"

            sendRsp(context, msg)
        }
    }

    class AllArg(root: String) : AbstractCommand("$root.all") {

        init {
            name = "all"
        }

        override suspend fun execute(context: CommandContext) {
            val timespan: Pair<Long, Long> = getTimespanFromArgNMessage(context, 0)
            val wrapper = context.daoManager.commandUsageWrapper
            val result = wrapper.getTopUsageWithinPeriod(timespan.first, timespan.second, -1)

            var msg = "```INI"
            for ((cmd, usageCount) in result) {
                msg += "\n$usageCount - [${cmd.name}]"
            }
            msg += "```"

            sendRsp(context, msg)
        }
    }
}