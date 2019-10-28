package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getDurationString
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg

class QueueCommand : AbstractCommand("command.queue") {

    init {
        id = 82
        name = "queue"
        aliases = arrayOf("q", "list", "songlist", "songs", "tracks")
        commandCategory = CommandCategory.MUSIC

    }

    override suspend fun execute(context: CommandContext) {
        val trackManager = context.musicPlayerManager.getGuildMusicPlayer(context.getGuild()).guildTrackManager
        val allTracks = trackManager.tracks.toMutableList()

        var description = ""


        val cTrack: AudioTrack? = trackManager.iPlayer.playingTrack
        if (cTrack == null) {
            val noSongPlaying = i18n.getTranslation(context, "message.music.notracks")
            sendMsg(context, noSongPlaying)
            return
        }
        var totalDuration = cTrack.duration

        val status =  i18n.getTranslation(context, if (trackManager.iPlayer.isPaused) "paused" else "playing")
        description += "[$status](${cTrack.info.uri})  **${cTrack.info.title}**  `[${getDurationString(trackManager.iPlayer.trackPosition)} / ${getDurationString(cTrack.duration)}]`"
        for ((index, track) in allTracks.withIndex()) {
            totalDuration += track.duration
            description += "\n[#${index + 1}](${track.info.uri})  ${track.info.title}  `[${getDurationString(track.duration)}]`"
        }

        val title = i18n.getTranslation(context, "$root.title")
            .replace("%amount%", (allTracks.size + 1).toString())
        val footer = i18n.getTranslation(context, "$root.footer")
            .replace("%duration%", getDurationString(totalDuration))

        val eb = Embedder(context)
        eb.setTitle(title)
        eb.setDescription(description)
        eb.setFooter(footer)
        sendEmbed(context, eb.build())
    }
}