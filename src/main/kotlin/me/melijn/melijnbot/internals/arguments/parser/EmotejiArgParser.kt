package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.SupportedDiscordEmoji
import net.dv8tion.jda.api.entities.Emote

class EmotejiArgParser : CommandArgParser<Pair<Emote?, String?>>() {

    private val emoteArgParser = EmoteArgParser()

    override suspend fun parse(context: ICommandContext, arg: String): Pair<Emote?, String?>? {
        val emoji = if (SupportedDiscordEmoji.helpMe.contains(arg)) {
            arg
        } else {
            null
        }

        val emote = if (emoji == null) {
            emoteArgParser.parse(context, arg)
        } else null

        if (emoji == null && emote == null) {
            return null
        }
        return Pair(emote, emoji)
    }
}