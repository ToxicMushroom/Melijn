package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.getTimeFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

class SeekCommand : AbstractCommand("command.seek") {

    init {
        id = 89
        name = "seek"
        aliases = arrayOf("scrub", "seekTo")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    suspend fun execute(context: ICommandContext) {
        val iPlayer = context.getGuildMusicPlayer().guildTrackManager.iPlayer
        val track = iPlayer.playingTrack ?: throw IllegalArgumentException("checks failed")
        val trackDuration = track.duration
        var trackPosition = iPlayer.trackPosition

        if (!track.isSeekable) {
            val msg = context.getTranslation("$root.notseekable")
            sendRsp(context, msg)
            return
        }

        val msg = if (context.args.isEmpty()) {
            context.getTranslation("$root.show")
        } else {
            trackPosition = getTimeFromArgsNMessage(context, 0, trackDuration) ?: return

            iPlayer.seekTo(trackPosition)
            context.getTranslation("$root.seeked")

        }
            .withVariable("duration", getDurationString(trackDuration))
            .withVariable("position", getDurationString(trackPosition))

        sendRsp(context, msg)
    }
}