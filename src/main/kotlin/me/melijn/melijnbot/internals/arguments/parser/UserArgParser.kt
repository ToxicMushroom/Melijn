package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.DISCORD_ID
import me.melijn.melijnbot.internals.utils.USER_MENTION
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.getUserByTag
import net.dv8tion.jda.api.entities.User

class UserArgParser : CommandArgParser<User?>() {

    override suspend fun parse(context: ICommandContext, argument: String): User? {
        val user1: User? = getUserByTag(context.shardManager, argument)
        return when {
            user1 != null -> user1
            else -> {
                if (argument.isBlank()) null
                else when {
                    DISCORD_ID.matches(argument) -> {
                        context.shardManager.retrieveUserById(argument).awaitOrNull()
                    }
                    USER_MENTION.matches(argument) -> {
                        val id = (USER_MENTION.find(argument) ?: return null).groupValues[1]
                        context.message.mentionedUsers.firstOrNull { it.id == id }
                            ?: context.shardManager.retrieveUserById(id).awaitOrNull()
                    }
                    else -> context.guildN?.retrieveMembersByPrefix(argument, 1)?.awaitOrNull()?.firstOrNull()?.user
                }
            }
        }
    }
}