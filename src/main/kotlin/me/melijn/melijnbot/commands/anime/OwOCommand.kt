package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.commandutil.anime.AnimeCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext

class OwOCommand : AbstractCommand("command.owo") {

    init {
        id = 103
        name = "owo"
        commandCategory = CommandCategory.ANIME
    }

    override suspend fun execute(context: CommandContext) {
        AnimeCommandUtil.executeShow(context, "owo")
    }
}