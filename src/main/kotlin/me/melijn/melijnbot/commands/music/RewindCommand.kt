package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getDurationString
import me.melijn.melijnbot.objects.utils.getTimeFromArgsNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class RewindCommand : AbstractCommand("command.rewind") {

    init {
        id = 93
        name = "rewind"
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val iPlayer = context.getGuildMusicPlayer().guildTrackManager.iPlayer
        val track = iPlayer.playingTrack
        val trackDuration = track.duration
        var trackPosition = iPlayer.trackPosition


        val msg = if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        } else {
            trackPosition -= getTimeFromArgsNMessage(context, 0, trackDuration) ?: return
            iPlayer.seekTo(trackPosition)
            i18n.getTranslation(context, "$root.rewinded")

        }
            .replace("%duration%", getDurationString(trackDuration))
            .replace("%position%", getDurationString(trackPosition))

        sendMsg(context, msg)
    }
}