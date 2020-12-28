package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp

class SpamCommand : AbstractCommand("command.spam") {

    init {
        id = 169
        name = "spam"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: ICommandContext) {
        val bool = getBooleanFromArgNMessage(context, 0) ?: return
        val msg = "blub exception logs have been "

        context.container.logToDiscord = bool
        if (bool) {
            sendRsp(context, msg + "enabled")
        } else {
            sendRsp(context, msg + "disabled")
        }
    }
}