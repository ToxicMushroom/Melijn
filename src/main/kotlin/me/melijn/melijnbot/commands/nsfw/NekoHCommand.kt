package me.melijn.melijnbot.commands.nsfw

import me.melijn.melijnbot.commandutil.anime.AnimeCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext

class NekoHCommand : AbstractCommand("command.nekoh") {

    init {
        id = 228
        name = "nekoH"
        commandCategory = CommandCategory.NSFW
    }

    override suspend fun execute(context: CommandContext) {
        AnimeCommandUtil.executeShow(context, "neko", true)
    }
}