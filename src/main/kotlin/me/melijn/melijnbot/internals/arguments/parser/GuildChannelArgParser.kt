package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.CHANNEL_MENTION
import me.melijn.melijnbot.internals.utils.DISCORD_ID
import net.dv8tion.jda.api.entities.GuildChannel

class GuildChannelArgParser : CommandArgParser<GuildChannel>() {

    override suspend fun parse(context: ICommandContext, arg: String): GuildChannel? {
        return if (DISCORD_ID.matches(arg)) {
            context.shardManager.getGuildChannelById(arg)

        } else if (context.isFromGuild && context.guild.getTextChannelsByName(arg, true).size > 0) {
            context.guild.getTextChannelsByName(arg, true)[0]

        } else if (context.isFromGuild && context.guild.getVoiceChannelsByName(arg, true).size > 0) {
            context.guild.getVoiceChannelsByName(arg, true)[0]

        } else if (context.isFromGuild && context.guild.getStoreChannelsByName(arg, true).size > 0) {
            context.guild.getStoreChannelsByName(arg, true)[0]

        } else if (CHANNEL_MENTION.matches(arg)) {
            val id = (CHANNEL_MENTION.find(arg) ?: return null).groupValues[1]
            context.message.mentionedChannels.firstOrNull { it.id == id }
                ?: context.shardManager.getGuildChannelById(id)

        } else null
    }
}