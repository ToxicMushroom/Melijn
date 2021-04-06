package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext

class IntArgParser : CommandArgParser<Int>() {

    override suspend fun parse(context: ICommandContext, arg: String): Int? {
        return arg.toIntOrNull()
    }
}