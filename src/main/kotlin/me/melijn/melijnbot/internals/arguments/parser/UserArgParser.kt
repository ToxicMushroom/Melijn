package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.DISCORD_ID
import me.melijn.melijnbot.internals.utils.USER_MENTION
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.getUserByTag
import net.dv8tion.jda.api.entities.User

class UserArgParser : CommandArgParser<User>() {

    override suspend fun parse(context: ICommandContext, arg: String): User? {
        val user1: User? = getUserByTag(context.shardManager, arg)
        return when {
            user1 != null -> user1
            else -> {
                if (arg.isBlank()) null
                else when {
                    DISCORD_ID.matches(arg) -> {
                        context.shardManager.retrieveUserById(arg).awaitOrNull()
                    }
                    USER_MENTION.matches(arg) -> {
                        val id = (USER_MENTION.find(arg) ?: return null).groupValues[1]
                        context.message.mentionedUsers.firstOrNull { it.id == id }
                            ?: context.shardManager.retrieveUserById(id).awaitOrNull()
                    }
                    else -> context.guildN?.retrieveMembersByPrefix(arg, 1)?.awaitOrNull()?.firstOrNull()?.user
                }
            }
        }
    }
}