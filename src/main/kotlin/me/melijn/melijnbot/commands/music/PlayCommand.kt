package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition

class PlayCommand : AbstractCommand("command.play") {

    init {
        id = 80
        name = "play"
        aliases = arrayOf("p")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {

    }
}