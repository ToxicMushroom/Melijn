package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext

class MusicNodeCommand : AbstractCommand("command.musicnode") {

    init {
        id = 143
        name = "musicNode"
        aliases = arrayOf("mn")
        commandCategory = CommandCategory.MUSIC
    }


    override suspend fun execute(context: CommandContext) {
        // TODO show current node info
    }
}

