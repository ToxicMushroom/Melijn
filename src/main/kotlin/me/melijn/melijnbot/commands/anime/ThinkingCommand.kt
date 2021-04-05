package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.commandutil.anime.AnimeCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext

class ThinkingCommand : AbstractCommand("command.thinking") {

    init {
        id = 109
        name = "thinking"
        aliases = arrayOf("think")
        commandCategory = CommandCategory.ANIME
    }

    suspend fun execute(context: ICommandContext) {
        AnimeCommandUtil.executeShow(context, "thinking")
    }
}