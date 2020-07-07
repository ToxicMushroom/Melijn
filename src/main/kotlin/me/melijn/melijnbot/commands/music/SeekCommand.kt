package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.getDurationString
import me.melijn.melijnbot.objects.utils.getTimeFromArgsNMessage
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.withVariable

class SeekCommand : AbstractCommand("command.seek") {

    init {
        id = 89
        name = "seek"
        aliases = arrayOf("scrub", "seekTo")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer
        val track = iPlayer.playingTrack
        val trackDuration = track.duration
        var trackPosition = iPlayer.trackPosition


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