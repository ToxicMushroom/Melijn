package me.melijn.melijnbot.internals.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.ConsoleColor
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.threading.TaskManager
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent

class BotJoinLeaveListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildJoinEvent) {
            onBotJoinGuild(event)
        } else if (event is GuildLeaveEvent) {
            TaskManager.async(event.guild) {
                onBotLeaveGuild(event)
            }
        }
    }

    private suspend fun onBotLeaveGuild(event: GuildLeaveEvent) {
        container.lavaManager.closeConnection(event.guild.idLong)

        logger.info(
            "{}Left the '{}' guild, id: {}, shard: {}{}",
            ConsoleColor.BLUE,
            event.guild.name,
            event.guild.id,
            event.jda.shardInfo.shardId,
            ConsoleColor.RESET
        )
    }

    private fun onBotJoinGuild(event: GuildJoinEvent) {
        logger.info(
            "{}Joined the '{}' guild, id: {}, shard: {}{}",
            ConsoleColor.CYAN,
            event.guild.name,
            event.guild.id,
            event.jda.shardInfo.shardId,
            ConsoleColor.RESET
        )
    }
}