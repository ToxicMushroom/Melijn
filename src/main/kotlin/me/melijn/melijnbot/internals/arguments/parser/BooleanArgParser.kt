package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext

class BooleanArgParser : CommandArgParser<Boolean>() {

    override suspend fun parse(context: ICommandContext, arg: String): Boolean? {
        return when (arg.toLowerCase()) {
            "true", "yes", "enable", "enabled", "allow", "allowed" -> true
            "false", "no", "disable", "disabled", "deny", "denied" -> true
            else -> null
        }
    }
}