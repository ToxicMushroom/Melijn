package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.commandutil.anime.AnimeCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext

class KissCommand : AbstractCommand("command.kiss") {

    init {
        id = 62
        name = "kiss"
        commandCategory = CommandCategory.ANIME
    }

    override suspend fun execute(context: CommandContext) {
        AnimeCommandUtil.execute(context, "kiss")
    }
}