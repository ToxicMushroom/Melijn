package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.commandutil.anime.AnimeCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext

class MeguminCommand : AbstractCommand("command.megumin") {

    init {
        id = 104
        name = "megumin"
        aliases = arrayOf("megu", "bakuretsu")
        commandCategory = CommandCategory.ANIME
    }

    suspend fun execute(context: ICommandContext) {
        AnimeCommandUtil.executeShow(context, "megumin")
    }
}