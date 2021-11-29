package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.message.sendRsp
import kotlin.time.Duration.Companion.milliseconds

class MusicPlayerCommand : AbstractCommand("command.musicplayer") {

    init {
        name = "musicPlayer"
        runConditions = arrayOf(RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory =CommandCategory.MUSIC
    }

    override suspend fun execute(context: ICommandContext) {
        val mp = context.getGuildMusicPlayer()
        val tm = mp.guildTrackManager
        var msg = "**Group:** ${mp.groupId}\n" +
            "**Playing:** ${tm.playingTrack?.info?.title}\n" +
            "**Queue:** ${tm.trackSize()}\n" +
            "**Looped|LoopedQueue:** ${tm.loopedTrack}|${tm.loopedQueue}\n" +
            "**Paused:** ${tm.iPlayer.paused}\n"
        val stats = context.lavaManager.jdaLavaLink?.getExistingLink(context.guildId)?.getNode()?.stats
        stats?.let {
            msg += "**Playing/Players:** ${it.playingPlayers}/${it.players}\n" +
                "**CPUs/Load:** ${it.cpuCores}/${it.lavalinkLoad}\n" +
                "**Used/Memory:** ${it.memUsed}/${it.memAllocated}\n" +
                "**avg (Deficit/Nulled/Sent) per minute:** ${it.avgFramesDeficitPerMinute}/${it.avgFramesNulledPerMinute}/${it.avgFramesSentPerMinute}\n" +
                "**Uptime:** ${it.uptime.milliseconds.toIsoString()}\n"
        }
        sendRsp(context, msg)
    }
}