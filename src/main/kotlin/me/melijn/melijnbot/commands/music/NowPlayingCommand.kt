package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.utils.getDurationString
import me.melijn.melijnbot.objects.utils.sendEmbed

class NowPlayingCommand : AbstractCommand("command.nowplaying") {

    init {
        id = 88
        name = "nowPlaying"
        aliases = arrayOf("np", "playing", "currentTrack", "currentSong")
        runConditions = arrayOf(RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val trackManager = context.guildMusicPlayer.guildTrackManager
        val playingTrack = trackManager.iPlayer.playingTrack
        val trackStatus = context.getTranslation(if (trackManager.iPlayer.isPaused) "paused" else "playing")
        val looped = context.getTranslation("looped")
        val status = context.getTranslation("$root.status")
        val title = context.getTranslation("$root.title")
            .replace("%status%", trackStatus)

        val description = context.getTranslation("$root.show.description")
            .replace("%title%", playingTrack.info.title)
            .replace("%url%", playingTrack.info.uri)
        val progressField = context.getTranslation("$root.progress")
        val thumbnail = "https://img.youtube.com/vi/${playingTrack.identifier}/hqdefault.jpg"

        val eb = Embedder(context)
        eb.setTitle(title)
        eb.setThumbnail(thumbnail)
        eb.setDescription(description)
        eb.addField(progressField, getProgressBar(trackManager.iPlayer.playingTrack, trackManager.iPlayer.trackPosition), false)
        eb.addField(status, "**$trackStatus" + if (trackManager.loopedTrack) "** & **$looped**" else "", false)
        sendEmbed(context, eb.build())
    }
}

fun getProgressBar(playingTrack: AudioTrack, playerPosition: Long): String {
    if (playingTrack.info.isStream) {
        return "**" + getDurationString(playerPosition) + " | \uD83D\uDD34 Live**"
    }
    val percent = (playerPosition.toDouble() / playingTrack.duration.toDouble() * 18.0).toInt()
    val sb = StringBuilder("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
    sb.insert(percent, "](${playingTrack.info.uri + "&t=" + (playerPosition / 1000)})<a:cool_nyan:490978764264570894>")
    sb.append(" **").append(getDurationString(playerPosition)).append("/").append(getDurationString(playingTrack.duration)).append("**")
    sb.insert(0, "[")
    return sb.toString()
}