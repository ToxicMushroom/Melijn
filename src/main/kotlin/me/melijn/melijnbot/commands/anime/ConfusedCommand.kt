package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.commandutil.anime.AnimeCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.web.apis.WeebApi

class ConfusedCommand : AbstractCommand("command.confused") {

    init {
        id = 247
        name = "confused"
        commandCategory = CommandCategory.ANIME
    }

    override suspend fun execute(context: ICommandContext) {
        AnimeCommandUtil.execute(context, "confused", arrayOf(WeebApi.Type.MIKI))
    }
}