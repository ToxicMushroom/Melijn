package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.message.sendRsp

class MusicPlayerCommand : AbstractCommand("command.musicplayer") {

    init {
        name = "musicPlayer"
        runConditions = arrayOf(RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory =CommandCategory.MUSIC
    }

    override suspend fun execute(context: ICommandContext) {
        val mp = context.getGuildMusicPlayer()
        val tm = mp.guildTrackManager
        sendRsp(context, "**Group:** ${mp.groupId}\n" +
            "**Playing:** ${tm.playingTrack?.info?.title}\n" +
            "**Queue:** ${tm.trackSize()}\n" +
            "**Looped|LoopedQueue:** ${tm.loopedTrack}|${tm.loopedQueue}\n" +
            "**Paused:** ${tm.iPlayer.paused}\n")
    }
}