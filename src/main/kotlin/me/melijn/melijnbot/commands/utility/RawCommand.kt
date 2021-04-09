package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.arguments.ArgumentMode
import me.melijn.melijnbot.internals.arguments.annotations.CommandArg
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp

class RawCommand : AbstractCommand("command.raw") {

    init {
        id = 118
        name = "raw"
        commandCategory = CommandCategory.UTILITY
    }

    suspend fun execute(
        context: ICommandContext,
        @CommandArg(index = 0, mode = ArgumentMode.RAW) raw: String
    ) {
        sendRsp(context, "```${raw.replace("`", "'")}```")
    }
}