package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.getDurationString
import me.melijn.melijnbot.objects.utils.getTimeFromArgsNMessage
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.message.sendSyntax
import me.melijn.melijnbot.objects.utils.withVariable

class RewindCommand : AbstractCommand("command.rewind") {

    init {
        id = 93
        name = "rewind"
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer
        val track = iPlayer.playingTrack
        val trackDuration = track.duration
        var trackPosition = iPlayer.trackPosition

        trackPosition -= getTimeFromArgsNMessage(context, 0, trackDuration) ?: return
        iPlayer.seekTo(trackPosition)

        val msg = context.getTranslation("$root.rewinded")
            .withVariable("duration", getDurationString(trackDuration))
            .withVariable("position", getDurationString(trackPosition))

        sendRsp(context, msg)
    }
}