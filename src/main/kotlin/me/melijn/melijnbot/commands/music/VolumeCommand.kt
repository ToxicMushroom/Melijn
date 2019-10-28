package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandContext

class VolumeCommand : AbstractCommand("command.volume") {

    init {
        id = 87
        name = "volume"
    }

    override suspend fun execute(context: CommandContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}