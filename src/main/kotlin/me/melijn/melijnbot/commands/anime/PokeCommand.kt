package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.commandutil.anime.AnimeCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext

class PokeCommand : AbstractCommand("command.poke") {

    init {
        id = 73
        name = "poke"
        commandCategory = CommandCategory.ANIME
    }

    override suspend fun execute(context: ICommandContext) {
        AnimeCommandUtil.execute(context, "poke")
    }
}