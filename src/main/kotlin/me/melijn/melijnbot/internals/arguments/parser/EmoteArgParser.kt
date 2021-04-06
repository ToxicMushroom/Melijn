package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.DISCORD_ID
import me.melijn.melijnbot.internals.utils.EMOTE_MENTION
import net.dv8tion.jda.api.entities.Emote

class EmoteArgParser : CommandArgParser<Emote>() {

    override suspend fun parse(context: ICommandContext, arg: String): Emote? {
        var emote: Emote? = null

        if (DISCORD_ID.matches(arg)) {
            emote = context.shardManager.getEmoteById(arg)

        } else if (EMOTE_MENTION.matches(arg)) {
            val id = (EMOTE_MENTION.find(arg) ?: return null).groupValues[2]
            emote = context.message.emotes.firstOrNull { it.id == id }
                ?: context.shardManager.getEmoteById(id)

        } else {
            var emotes: List<Emote>? = context.guildN?.getEmotesByName(arg, false)
            if (emotes?.isNotEmpty() == true) emote = emotes[0]

            emotes = context.guildN?.getEmotesByName(arg, true)
            if (emotes != null && emotes.isNotEmpty() && emote == null) emote = emotes[0]

            emotes = context.guildN?.getEmotesByName(arg, false) ?: emptyList()
            if (emotes.isNotEmpty() && emote == null) emote = emotes[0]

            emotes = context.guildN?.getEmotesByName(arg, true) ?: emptyList()
            if (emotes.isNotEmpty() && emote == null) emote = emotes[0]

        }

        return emote
    }
}