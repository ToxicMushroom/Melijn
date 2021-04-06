package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext

class TextArgParser : CommandArgParser<String>() {

    override suspend fun parse(context: ICommandContext, arg: String): String {
        return arg
    }
}