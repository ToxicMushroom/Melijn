package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg

class SpamCommand : AbstractCommand("command.spam") {

    init {
        id = 169
        name = "spam"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        val bool = getBooleanFromArgNMessage(context, 0) ?: return
        val msg = "blub exception logs have been "

        context.container.logToDiscord = bool
        if (bool) {
            sendMsg(context, msg + "enabled")
        } else {
            sendMsg(context, msg + "disabled")
        }
    }
}