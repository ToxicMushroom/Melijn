package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.commandutil.anime.AnimeCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext

class BlushCommand : AbstractCommand("command.blush") {
    init {
        id = 58
        name = "blush"
        aliases = arrayOf("flushed")
        commandCategory = CommandCategory.ANIME
    }

    override suspend fun execute(context: ICommandContext) {
        AnimeCommandUtil.execute(context, "blush")
    }
}