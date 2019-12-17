package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.sendMsg

class LoopCommand : AbstractCommand("command.loop") {

    init {
        id = 90
        name = "loop"
        aliases = arrayOf("repeat", "repeatTrack", "trackRepeat", "loopTrack", "trackLoop")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val trackManager = context.guildMusicPlayer.guildTrackManager
        trackManager.loopedTrack = !trackManager.loopedTrack

        val extra = if (trackManager.loopedTrack) {
            "looped"
        } else {
            "unlooped"
        }
        val msg = context.getTranslation("$root.$extra")
        sendMsg(context, msg)
    }
}