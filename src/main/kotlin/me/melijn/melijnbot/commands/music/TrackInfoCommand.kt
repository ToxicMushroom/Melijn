package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.music.TrackUserData
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withSafeVariable

class TrackInfoCommand : AbstractCommand("command.trackinfo") {

    init {
        id = 96
        name = "trackInfo"
        runConditions = arrayOf(RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    suspend fun execute(context: ICommandContext) {
        val player = context.getGuildMusicPlayer()
        val trackManager = player.guildTrackManager
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val playingTrack = trackManager.iPlayer.playingTrack ?: throw IllegalArgumentException("checks failed")

        val targetIndex = getIntegerFromArgNMessage(context, 0, 0, trackManager.tracks.size) ?: return
        val targetTrack = if (targetIndex == 0) {
            playingTrack
        } else {
            trackManager.tracks.get(targetIndex - 1)
        }

        val trackUserData = (targetTrack.userData as TrackUserData)
        val title = context.getTranslation("$root.title")
        val requester = context.getTranslation("$root.requester")
        val requesterId = context.getTranslation("$root.requesterid")
        val length = context.getTranslation("$root.length")
        val timeuntil = context.getTranslation("$root.timeuntil")
        val progress = context.getTranslation("$root.progress")
        val desc = "**[%title%](${targetTrack.info.uri})**"
            .withSafeVariable("title", targetTrack.info.title)

        var timeUntilTime = playingTrack.duration - trackManager.iPlayer.trackPosition
        trackManager.tracks.indexedForEach { index, track ->
            if (index < (targetIndex - 1)) {
                timeUntilTime += track.duration
            }
        }
        if (trackManager.loopedTrack) timeUntilTime = Long.MAX_VALUE
        if (targetIndex == 0) timeUntilTime = 0


        val embedder = Embedder(context)
            .setTitle(title)
            .setDescription(desc)
            .addField(requester, trackUserData.userTag, true)
            .addField(requesterId, trackUserData.userId.toString(), true)
            .setThumbnail("https://img.youtube.com/vi/${targetTrack.identifier}/hqdefault.jpg")

        if (targetIndex != 0) {
            embedder.addField(length, "`[${getDurationString(targetTrack.duration)}]`", true)
            embedder.addField(timeuntil, getDurationString(timeUntilTime), false)
        } else {
            embedder.addField(progress, getProgressBar(targetTrack, trackManager.iPlayer.trackPosition), false)
        }

        sendEmbedRsp(context, embedder.build())
    }
}