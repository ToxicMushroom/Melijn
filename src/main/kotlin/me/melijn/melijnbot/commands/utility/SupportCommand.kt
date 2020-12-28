package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp

class SupportCommand : AbstractCommand("command.support") {

    init {
        id = 148
        name = "support"
        aliases = arrayOf("supportServer")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val msg = context.getTranslation("$root.server")
        sendRsp(context, msg)
    }
}