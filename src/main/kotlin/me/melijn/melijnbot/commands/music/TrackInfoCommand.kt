package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.music.TrackUserData
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import java.lang.Integer.max

class TrackInfoCommand : AbstractCommand("command.trackinfo") {

    init {
        id = 96
        name = "trackInfo"
        runConditions = arrayOf(RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val player = context.getGuildMusicPlayer()
        val trackManager = player.guildTrackManager
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val index = getIntegerFromArgNMessage(context, 0, 0, trackManager.trackSize()) ?: return

        val playingTrack = trackManager.iPlayer.playingTrack ?: throw IllegalArgumentException("checks failed")
        val track = if (index == 0) {
            playingTrack
        } else {
            trackManager.tracks[index - 1]
        }

        val trackUserData = (track.userData as TrackUserData)
        val title = context.getTranslation("$root.title")
        val requester = context.getTranslation("$root.requester")
        val requesterId = context.getTranslation("$root.requesterid")
        val length = context.getTranslation("$root.length")
        val timeuntil = context.getTranslation("$root.timeuntil")
        val progress = context.getTranslation("$root.progress")
        val desc = "**[${track.info.title}](${track.info.uri})**"
        var timeUntilTime = playingTrack.duration - trackManager.iPlayer.trackPosition
        trackManager.tracks.toList().subList(0, max(index - 1, 0)).forEach { tr -> timeUntilTime += tr.duration }
        if (trackManager.loopedTrack) timeUntilTime = Long.MAX_VALUE
        if (index == 0) timeUntilTime = 0


        val embedder = Embedder(context)
            .setTitle(title)
            .setDescription(desc)
            .addField(requester, trackUserData.userTag, true)
            .addField(requesterId, trackUserData.userId.toString(), true)
            .setThumbnail("https://img.youtube.com/vi/${track.identifier}/hqdefault.jpg")

        if (index != 0) {
            embedder.addField(length, "`[${getDurationString(track.duration)}]`", true)
            embedder.addField(timeuntil, getDurationString(timeUntilTime), false)
        } else {
            embedder.addField(progress, getProgressBar(track, trackManager.iPlayer.trackPosition), false)
        }

        sendEmbedRsp(context, embedder.build())
    }
}