package me.melijn.melijnbot.commandutil.game

import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.DISCORD_ID
import me.melijn.melijnbot.internals.utils.USER_MENTION
import me.melijn.melijnbot.internals.utils.awaitOrNull
import net.dv8tion.jda.api.entities.User

object OsuUtil {
    suspend fun retrieveDiscordUserForOsuByArgsN(context: ICommandContext, index: Int): User? {
        return when {
            context.args.size > index -> {
                val arg = context.args[index]

                when {
                    DISCORD_ID.matches(arg) -> {
                        context.shardManager.retrieveUserById(arg).awaitOrNull()
                    }
                    USER_MENTION.matches(arg) -> {
                        val id = (USER_MENTION.find(arg) ?: return null).groupValues[1]
                        context.message.mentionedUsers.firstOrNull { it.id == id } ?:
                        context.shardManager.retrieveUserById(id).awaitOrNull()
                    }
                    else -> null
                }
            }
            else -> null
        }
    }
}