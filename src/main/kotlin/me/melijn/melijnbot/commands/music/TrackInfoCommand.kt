package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.music.TrackUserData
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getDurationString
import me.melijn.melijnbot.objects.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendSyntax
import java.lang.Integer.max

class TrackInfoCommand : AbstractCommand("command.trackinfo") {

    init {
        id = 96
        name = "trackInfo"
        runConditions = arrayOf(RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val player = context.guildMusicPlayer
        val trackManager = player.guildTrackManager
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val index = getIntegerFromArgNMessage(context, 0, 0, trackManager.trackSize()) ?: return

        val track = if (index == 0) {
            trackManager.iPlayer.playingTrack
        } else {
            trackManager.tracks.toList()[index - 1]
        }

        val trackUserData = (track.userData as TrackUserData)
        val title = i18n.getTranslation(context, "$root.title")
        val requester = i18n.getTranslation(context, "$root.requester")
        val requesterId = i18n.getTranslation(context, "$root.requesterid")
        val length = i18n.getTranslation(context, "$root.length")
        val timeuntil = i18n.getTranslation(context, "$root.timeuntil")
        val progress = i18n.getTranslation(context, "$root.progress")
        val desc = "**[${track.info.title}](${track.info.uri})**"
        var timeUntilTime = trackManager.iPlayer.playingTrack.duration - trackManager.iPlayer.trackPosition
        trackManager.tracks.toList().subList(0, max(index - 1, 0)).forEach { tr -> timeUntilTime += tr.duration }
        if (trackManager.loopedTrack) timeUntilTime = Long.MAX_VALUE
        if (index == 0) timeUntilTime = 0


        val embedder = Embedder(context)
        embedder.setTitle(title)
        embedder.setDescription(desc)
        embedder.addField(requester, trackUserData.userTag, true)
        embedder.addField(requesterId, trackUserData.userId.toString(), true)
        embedder.setThumbnail("https://img.youtube.com/vi/${track.identifier}/hqdefault.jpg")
        if (index != 0) {
            embedder.addField(length, "`[${getDurationString(track.duration)}]`", true)
            embedder.addField(timeuntil, getDurationString(timeUntilTime), false)
        } else {
            embedder.addField(progress, getProgressBar(track, trackManager.iPlayer.trackPosition), false)
        }

        sendEmbed(context, embedder.build())
    }
}