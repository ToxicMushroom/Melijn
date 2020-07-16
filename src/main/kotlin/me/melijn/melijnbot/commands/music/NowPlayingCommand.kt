package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.withVariable

class NowPlayingCommand : AbstractCommand("command.nowplaying") {

    init {
        id = 88
        name = "nowPlaying"
        aliases = arrayOf("np", "playing", "currentTrack", "currentSong")
        runConditions = arrayOf(RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val trackManager = context.getGuildMusicPlayer().guildTrackManager
        val playingTrack = trackManager.iPlayer.playingTrack ?: throw IllegalArgumentException("Checks failed")
        val trackStatus = context.getTranslation(if (trackManager.iPlayer.paused) "paused" else "playing")
        val looped = context.getTranslation("looped")
        val status = context.getTranslation("$root.status")
        val title = context.getTranslation("$root.title")
            .withVariable("status", trackStatus)

        val description = context.getTranslation("$root.show.description")
            .withVariable("title", playingTrack.info.title)
            .withVariable("url", playingTrack.info.uri)
        val progressField = context.getTranslation("$root.progress")
        val thumbnail = "https://img.youtube.com/vi/${playingTrack.identifier}/hqdefault.jpg"

        val eb = Embedder(context)
            .setTitle(title)
            .setThumbnail(thumbnail)
            .setDescription(description)
            .addField(progressField, getProgressBar(playingTrack, trackManager.iPlayer.trackPosition), false)
            .addField(status, "**$trackStatus**" + if (trackManager.loopedTrack) " & **$looped**" else "", false)
        sendEmbedRsp(context, eb.build())
    }
}

fun getProgressBar(playingTrack: AudioTrack, playerPosition: Long): String {
    if (playingTrack.info.isStream) {
        return "**" + getDurationString(playerPosition) + " | \uD83D\uDD34 Live**"
    }
    val percent = (playerPosition.toDouble() / playingTrack.duration.toDouble() * 18.0).toInt()
    return StringBuilder("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
        .insert(percent, "](${playingTrack.info.uri + "&t=" + (playerPosition / 1000)})<a:cool_nyan:490978764264570894>")
        .append(" **").append(getDurationString(playerPosition)).append("/").append(getDurationString(playingTrack.duration)).append("**")
        .insert(0, "[")
        .toString()
}