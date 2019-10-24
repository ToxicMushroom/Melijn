package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendMsg

class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        val top = context.daoManager.commandUsageWrapper.getTopUsageWithinPeriod(0, System.currentTimeMillis(), 3)
        var string = "title: "
        top.forEach { (t, u) ->
            string += "\n$t - $u"
        }
        sendMsg(context, string)
    }
}