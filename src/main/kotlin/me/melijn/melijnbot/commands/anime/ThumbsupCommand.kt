package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.commandutil.anime.AnimeCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext

class ThumbsupCommand : AbstractCommand("command.thumbsup") {

    init {
        id = 68
        name = "thumbsup"
        commandCategory = CommandCategory.ANIME
    }

    suspend fun execute(context: ICommandContext) {
        AnimeCommandUtil.execute(context, "thumbsup")
    }
}