package me.melijn.melijnbot.internals.arguments

import me.melijn.melijnbot.internals.command.ICommandContext

abstract class CommandArgParser<T> {
    abstract suspend fun parse(context: ICommandContext, argument: String): T
}