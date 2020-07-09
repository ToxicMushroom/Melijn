package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.commandutil.anime.AnimeCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext

class NekoCommand : AbstractCommand("command.neko") {

    init {
        id = 107
        name = "neko"
        commandCategory = CommandCategory.ANIME
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isNotEmpty() && context.args[0] == "nsfw") {
            AnimeCommandUtil.executeShow(context, "neko", true)
        } else AnimeCommandUtil.executeShow(context, "neko")
    }
}