package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp

class OenkCommand : AbstractCommand("command.oenk") {

    init {
        name = "oenk"
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val text = context.message.referencedMessage?.contentRaw ?: context.rawArg
        sendRsp(context, "oenk ".repeat(text.count { c -> c == ' ' } + 1))
    }

}