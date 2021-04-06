package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.awaitOrNull
import net.dv8tion.jda.api.entities.Member

class MemberArgParser : CommandArgParser<Member>() {

    private val userArgParser = UserArgParser()

    override suspend fun parse(context: ICommandContext, arg: String): Member? {
        val user = userArgParser.parse(context, arg)
        return if (user == null) null
        else context.guild.retrieveMember(user).awaitOrNull()
    }
}