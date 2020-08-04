package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.message.sendRsp

class LoopCommand : AbstractCommand("command.loop") {

    init {
        id = 90
        name = "loop"
        aliases = arrayOf("repeat", "repeatTrack", "trackRepeat", "loopTrack", "trackLoop")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL, RunCondition.VOTED)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val trackManager = context.getGuildMusicPlayer().guildTrackManager
        trackManager.loopedTrack = !trackManager.loopedTrack

        val extra = if (trackManager.loopedTrack) {
            "looped"
        } else {
            "unlooped"
        }

        val msg = context.getTranslation("$root.$extra")
        sendRsp(context, msg)
    }
}