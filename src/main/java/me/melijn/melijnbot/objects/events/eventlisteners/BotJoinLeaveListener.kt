package me.melijn.melijnbot.objects.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.ConsoleColor
import me.melijn.melijnbot.objects.events.AbstractListener
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent

class BotJoinLeaveListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildJoinEvent) {
            onBotJoinGuild(event)
        } else if (event is GuildLeaveEvent) {
            onBotLeaveGuild(event)
        }
    }

    private fun onBotLeaveGuild(event: GuildLeaveEvent) {
        logger.info("{}Joined the '{}' guild, id: {}, shard: {}",
                ConsoleColor.CYAN,
                event.guild.name,
                event.guild.id,
                event.jda.shardInfo.shardId)
    }

    private fun onBotJoinGuild(event: GuildJoinEvent) {
        logger.info("{}Left the '{}' guild, id: {}, shard: {}",
                ConsoleColor.BLUE,
                event.guild.name,
                event.guild.id,
                event.jda.shardInfo.shardId)
    }
}