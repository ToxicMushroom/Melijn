package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.*
import me.melijn.melijnbot.objects.utils.YTSearch
import me.melijn.melijnbot.objects.utils.getDurationString
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg

class AudioLoader(private val musicPlayerManager: MusicPlayerManager) {

    val root = "message.music"
    private val audioPlayerManager = musicPlayerManager.audioPlayerManager
    private val ytSearch = YTSearch()

    suspend fun loadNewTrackNMessage(context: CommandContext, source: String, isPlaylist: Boolean = false) {
        val guild = context.getGuild()
        val guildMusicPlayer = musicPlayerManager.getGuildMusicPlayer(guild)
        val guildTrackManager = guildMusicPlayer.guildTrackManager
        val rawInput = source
            .replace(YT_SELECTOR, "")
            .replace(SC_SELECTOR, "")

        val resultHandler = object : AudioLoadResultHandler {
            override fun loadFailed(exception: FriendlyException) {
                runBlocking {
                    val msg = i18n.getTranslation(context, "$root.loadfailed")
                        .replace("%cause%", exception.message ?: "/")
                    sendMsg(context, msg)
                }
            }

            override fun trackLoaded(track: AudioTrack) {
                track.userData = TrackUserData(context.getAuthor())
                guildTrackManager.queue(track)
                sendMessageAddedTrack(context, track)
            }

            override fun noMatches() {
                sendMessageNoMatches(context, rawInput)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                val tracks = playlist.tracks
                if (isPlaylist) {
                    for (track in tracks) {
                        track.userData = TrackUserData(context.getAuthor())

                        guildTrackManager.queue(track)
                    }
                    sendMessageAddedTracks(context, tracks)
                } else {
                    val track = tracks[0]
                    track.userData = TrackUserData(context.getAuthor())
                    guildTrackManager.queue(track)
                    sendMessageAddedTrack(context, track)
                }
            }
        }

        if (source.startsWith(YT_SELECTOR)) {
            ytSearch.search(rawInput) { videoId ->
                if (videoId == null) {
                    sendMessageNoMatches(context, rawInput)
                } else {
                    audioPlayerManager.loadItemOrdered(guildMusicPlayer, YT_VID_URL_BASE + videoId, resultHandler)
                }
            }
        } else {
            audioPlayerManager.loadItemOrdered(guildMusicPlayer, source, resultHandler)
        }
    }

    fun sendMessageNoMatches(context: CommandContext, input: String) = runBlocking {
        val msg = i18n.getTranslation(context, "$root.nomatches")
            .replace("%source%", input)
        sendMsg(context, msg)
    }


    fun sendMessageAddedTrack(context: CommandContext, audioTrack: AudioTrack) = runBlocking {
        val title = i18n.getTranslation(context, "$root.addedtrack.title")
            .replace(PLACEHOLDER_USER, context.getAuthor().asTag)
        val description = i18n.getTranslation(context, "$root.addedtrack.description")
            .replace("%position%", getQueuePosition(context, audioTrack).toString())
            .replace("%title%", audioTrack.info.title)
            .replace("%duration%", getDurationString(audioTrack.duration))
            .replace("%url%", audioTrack.info.uri)

        val eb = Embedder(context)
        eb.setTitle(title)
        eb.setDescription(description)

        sendEmbed(context, eb.build())
    }

    fun sendMessageAddedTracks(context: CommandContext, audioTracks: List<AudioTrack>) = runBlocking {
        val title = i18n.getTranslation(context, "$root.addedtracks.title")
            .replace(PLACEHOLDER_USER, context.getAuthor().asTag)
        val description = i18n.getTranslation(context, "$root.addedtracks.description")
            .replace("%size%", audioTracks.size.toString())
            .replace("%positionFirst%", getQueuePosition(context, audioTracks[0]).toString())
            .replace("%positionLast%", (getQueuePosition(context, audioTracks[0]) + audioTracks.size).toString())

        val eb = Embedder(context)
        eb.setTitle(title)
        eb.setDescription(description)

        sendEmbed(context, eb.build())
    }

    private fun getQueuePosition(context: CommandContext, audioTrack: AudioTrack): Int =
        context.musicPlayerManager.getGuildMusicPlayer(context.getGuild()).guildTrackManager.getPosition(audioTrack)


}