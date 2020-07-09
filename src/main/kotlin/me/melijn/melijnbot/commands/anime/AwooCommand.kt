package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.commandutil.anime.AnimeCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext

class AwooCommand : AbstractCommand("command.awoo") {

    init {
        id = 105
        name = "awoo"
        commandCategory = CommandCategory.ANIME
    }

    override suspend fun execute(context: CommandContext) {
        AnimeCommandUtil.execute(context, "awoo")
    }
}