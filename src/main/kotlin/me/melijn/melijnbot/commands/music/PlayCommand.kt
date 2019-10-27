package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext

class PlayCommand : AbstractCommand("command.play") {

    init {
        id = 80
        name = "play"
        aliases = arrayOf("p")
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        if (!context.lavaManager.isConnected(context.getGuild())) {
            context.getMember()?.voiceState?.channel?.let { context.lavaManager.openConnection(it) }
        }

        context.audioLoader.loadNewTrackNMessage(context, "ytsearch:" + context.rawArg, false)
    }
}