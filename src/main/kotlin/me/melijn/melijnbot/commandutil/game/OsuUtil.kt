package me.melijn.melijnbot.commandutil.game

import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.DISCORD_ID
import me.melijn.melijnbot.internals.utils.USER_MENTION
import me.melijn.melijnbot.internals.utils.awaitOrNull
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager

object OsuUtil {
    private fun getDiscordUserForOsuByArgsN(shardManager: ShardManager, arg: String): User? {
        return when {
            DISCORD_ID.matches(arg) -> {
                shardManager.getUserById(arg)
            }
            USER_MENTION.matches(arg) -> {
                shardManager.getUserById((USER_MENTION.find(arg) ?: return null).groupValues[1])
            }
            else -> null
        }
    }

    suspend fun retrieveDiscordUserForOsuByArgsN(context: CommandContext, index: Int): User? {
        val user1: User? = getDiscordUserForOsuByArgsN(context.shardManager, context.args[index])
        return when {
            user1 != null -> user1
            context.args.size > index -> {
                val arg = context.args[index]

                when {
                    DISCORD_ID.matches(arg) -> {
                        context.shardManager.retrieveUserById(arg).awaitOrNull()
                    }
                    USER_MENTION.matches(arg) -> {
                        val id = (USER_MENTION.find(arg) ?: return null).groupValues[1]
                        context.shardManager.retrieveUserById(id).awaitOrNull()
                    }
                    else -> null
                }
            }
            else -> null
        }

    }

}