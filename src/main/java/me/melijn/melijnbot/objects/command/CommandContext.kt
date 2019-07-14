package me.melijn.melijnbot.objects.command

import me.duncte123.botcommons.commands.ICommandContext
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class CommandContext(
        private val guildMessageReceivedEvent: GuildMessageReceivedEvent
) : ICommandContext {
    override fun getEvent(): GuildMessageReceivedEvent = guildMessageReceivedEvent
    override fun getGuild(): Guild = guildMessageReceivedEvent.guild
}