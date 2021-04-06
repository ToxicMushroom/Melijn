package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext

class LongArgParser : CommandArgParser<Long>() {

    override suspend fun parse(context: ICommandContext, arg: String): Long? {
        return arg.toLongOrNull()
    }
}