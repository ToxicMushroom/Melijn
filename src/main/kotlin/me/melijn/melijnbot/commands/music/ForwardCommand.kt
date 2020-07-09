package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.getTimeFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable

class ForwardCommand : AbstractCommand("command.forward") {

    init {
        id = 92
        name = "forward"
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer
        val track = iPlayer.playingTrack
        val trackDuration = track.duration
        var trackPosition = iPlayer.trackPosition


        val msg = if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        } else {
            trackPosition += getTimeFromArgsNMessage(context, 0, trackDuration) ?: return
            iPlayer.seekTo(trackPosition)
            context.getTranslation("$root.forwarded")

        }.withVariable("duration", getDurationString(trackDuration))
            .withVariable("position", getDurationString(trackPosition))

        sendRsp(context, msg)
    }
}