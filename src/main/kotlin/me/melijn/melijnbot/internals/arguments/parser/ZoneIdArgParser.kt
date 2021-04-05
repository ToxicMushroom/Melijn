package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext
import java.time.ZoneId

class ZoneIdArgParser : CommandArgParser<ZoneId?>() {

    override suspend fun parse(context: ICommandContext, argument: String): ZoneId? {
        return try {
            ZoneId.of(argument)
        } catch (t: Throwable) {
            null
        }
    }
}