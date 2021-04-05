package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.getTimeFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable

class RewindCommand : AbstractCommand("command.rewind") {

    init {
        id = 93
        name = "rewind"
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val iPlayer = context.getGuildMusicPlayer().guildTrackManager.iPlayer
        val track = iPlayer.playingTrack ?: throw IllegalArgumentException("checks failed")
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