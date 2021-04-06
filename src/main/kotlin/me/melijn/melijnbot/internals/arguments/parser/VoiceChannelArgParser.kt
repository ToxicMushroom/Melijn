package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.CHANNEL_MENTION
import me.melijn.melijnbot.internals.utils.DISCORD_ID
import net.dv8tion.jda.api.entities.VoiceChannel

class VoiceChannelArgParser : CommandArgParser<VoiceChannel>() {

    override suspend fun parse(context: ICommandContext, arg: String): VoiceChannel? {
        return if (DISCORD_ID.matches(arg)) {
            context.shardManager.getVoiceChannelById(arg)

        } else if (context.isFromGuild && context.guild.getVoiceChannelsByName(arg, true).size > 0) {
            context.guild.getVoiceChannelsByName(arg, true)[0]

        } else if (CHANNEL_MENTION.matches(arg)) {
            val id = (CHANNEL_MENTION.find(arg) ?: return null).groupValues[1]
            context.shardManager.getVoiceChannelById(id)

        } else null
    }
}