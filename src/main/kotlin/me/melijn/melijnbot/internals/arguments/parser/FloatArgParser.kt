package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext

class FloatArgParser : CommandArgParser<Float>() {

    override suspend fun parse(context: ICommandContext, arg: String): Float? {
        return arg.toFloatOrNull()
    }
}