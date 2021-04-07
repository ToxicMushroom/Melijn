package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.DISCORD_ID
import net.dv8tion.jda.api.entities.Guild

class GuildArgParser : CommandArgParser<Guild>() {

    override suspend fun parse(context: ICommandContext, arg: String): Guild? {
        return if (DISCORD_ID.matches(arg)) context.shardManager.getGuildById(arg)
        else null
    }
}