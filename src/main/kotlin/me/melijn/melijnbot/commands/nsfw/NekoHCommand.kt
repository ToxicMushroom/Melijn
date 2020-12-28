package me.melijn.melijnbot.commands.nsfw

import me.melijn.melijnbot.commandutil.anime.AnimeCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext

class NekoHCommand : AbstractCommand("command.nekoh") {

    init {
        id = 228
        name = "nekoH"
        commandCategory = CommandCategory.NSFW
    }

    override suspend fun execute(context: ICommandContext) {
        AnimeCommandUtil.executeShow(context, "neko", true)
    }
}