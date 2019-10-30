package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getDurationString
import me.melijn.melijnbot.objects.utils.sendMsg

class SeekCommand : AbstractCommand("command.seek") {

    init {
        id = 89
        name = "seek"
        aliases = arrayOf("scrub", "seekTo")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val iPlayer = context.getGuildMusicPlayer().guildTrackManager.iPlayer
        val track = iPlayer.playingTrack
        val trackDuration = track.duration
        var trackPosition = iPlayer.trackPosition


        val msg = if (context.args.isEmpty()) {
            i18n.getTranslation(context, "$root.show")
        } else {
            trackPosition = getTimeFromArgsNMessage(context, 0, trackDuration) ?: return
            iPlayer.seekTo(trackPosition)
            i18n.getTranslation(context, "$root.seeked")

        }
            .replace("%duration%", getDurationString(trackDuration))
            .replace("%position%", getDurationString(trackPosition))

        sendMsg(context, msg)
    }

    private suspend fun getTimeFromArgsNMessage(context: CommandContext, start: Long = Long.MIN_VALUE, end: Long = Long.MAX_VALUE): Long? {
        val parts = context.rawArg.replace(":", " ").split("\\s+".toRegex()).toMutableList()
        parts.reverse()
        var time: Long = 0
        var workingPart = ""
        try {
            for ((index, part) in parts.withIndex()) {
                workingPart = part
                time += part.toShort() * when (index) {
                    0 -> 1000
                    1 -> 60_000
                    2 -> 3_600_000
                    else -> 0
                }
            }
        } catch (ex: NumberFormatException) {
            val path =  if (workingPart.matches("\\d+".toRegex())) "message.numbertobig" else "message.unknown.number"
            val msg = i18n.getTranslation(context, path)
                .replace("%arg%", workingPart)
            sendMsg(context, msg)
            return null
        }
        if (start > time || end < time) {
            val msg = i18n.getTranslation(context, "command.seek.notinrange")
                .replace("%arg%", workingPart)
            sendMsg(context, msg)
            return null
        }
        return time
    }
}