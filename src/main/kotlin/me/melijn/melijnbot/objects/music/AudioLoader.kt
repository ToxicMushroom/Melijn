package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.wrapper.spotify.model_objects.specification.ArtistSimplified
import com.wrapper.spotify.model_objects.specification.PlaylistTrack
import com.wrapper.spotify.model_objects.specification.Track
import com.wrapper.spotify.model_objects.specification.TrackSimplified
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.*
import me.melijn.melijnbot.objects.utils.*
import java.lang.Integer.min

const val QUEUE_LIMIT = 150
const val DONATE_QUEUE_LIMIT = 1000

class AudioLoader(private val musicPlayerManager: MusicPlayerManager) {

    val root = "message.music"
    private val audioPlayerManager = musicPlayerManager.audioPlayerManager
    private val ytSearch = YTSearch()
    private val spotifyTrackDiff = 2000


    fun loadNewTrackNMessage(context: CommandContext, source: String, isPlaylist: Boolean = false) {
        val guild = context.getGuild()
        val guildMusicPlayer = musicPlayerManager.getGuildMusicPlayer(guild)
        val guildTrackManager = guildMusicPlayer.guildTrackManager
        val rawInput = source
            .replace(YT_SELECTOR, "")
            .replace(SC_SELECTOR, "")

        val resultHandler = object : AudioLoadResultHandler {
            override fun loadFailed(exception: FriendlyException) {
                sendMessageLoadFailed(context, exception)
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

    private fun sendMessageLoadFailed(context: CommandContext, exception: FriendlyException) = runBlocking {
        val msg = i18n.getTranslation(context, "$root.loadfailed")
            .replace("%cause%", exception.message ?: "/")
        sendMsg(context, msg)
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

    fun loadSpotifyTrack(
        context: CommandContext,
        query: String,
        artists: Array<ArtistSimplified>?,
        durationMs: Int,
        silent: Boolean = false,
        loaded: ((Boolean) -> Unit)? = null
    ) {
        val player: GuildMusicPlayer = context.getGuildMusicPlayer()
        val title: String = query.replaceFirst("$SC_SELECTOR|$YT_SELECTOR".toRegex(), "")
        val source = StringBuilder(query)
        val artistNames = mutableListOf<String>()
        appendArtists(artists, source, artistNames)
        audioPlayerManager.loadItemOrdered(player, source.toString(), object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                if ((durationMs + spotifyTrackDiff > track.duration && track.duration > durationMs - spotifyTrackDiff)
                    || track.info.title.contains(title, true)) {
                    if (player.safeQueue(context, track)) {
                        if (!silent) {
                            sendMessageAddedTrack(context, track)
                        }
                        loaded?.invoke(true)
                    } else {
                        loaded?.invoke(false)
                    }
                } else {
                    loadSpotifyTrackOther(context, query, artists, durationMs, title)
                }
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                val tracks: List<AudioTrack> = playlist.tracks
                for (track in tracks.subList(0, min(tracks.size, 5))) {
                    if ((durationMs + spotifyTrackDiff > track.duration && track.duration > durationMs - spotifyTrackDiff)
                        || track.info.title.contains(title, true)) {
                        if (player.safeQueue(context, track)) {
                            if (!silent) {
                                sendMessageAddedTrack(context, track)
                            }
                            loaded?.invoke(true)
                        } else {
                            loaded?.invoke(false)
                        }
                        return
                    }
                }
                loadSpotifyTrackOther(context, query, artists, durationMs, title)
            }

            override fun noMatches() {
                loadSpotifyTrackOther(context, query, artists, durationMs, title)
            }

            override fun loadFailed(exception: FriendlyException) {
                if (!silent) {
                    sendMessageLoadFailed(context, exception)
                }
                loaded?.invoke(false)
            }
        })
    }

    private fun loadSpotifyTrackOther(
        context: CommandContext,
        query: String,
        artists: Array<ArtistSimplified>?,
        durationMs: Int,
        title: String,
        silent: Boolean = false,
        loaded: ((Boolean) -> Unit)? = null
    ) {
        if (query.startsWith(YT_SELECTOR)) {
            if (artists != null) {
                loadSpotifyTrack(context, query, null, durationMs)
            } else {
                val newQuery = query.replaceFirst(YT_SELECTOR, SC_SELECTOR)
                loadSpotifyTrack(context, newQuery, artists, durationMs)
            }
        } else if (query.startsWith(SC_SELECTOR)) {
            if (artists != null) {
                loadSpotifyTrack(context, query, null, durationMs)
            } else {
                if (!silent) sendMessageNoMatches(context, title)
                loaded?.invoke(false)
            }
        }
    }

    private fun appendArtists(artists: Array<ArtistSimplified>?, source: StringBuilder, artistNames: MutableList<String>) {
        if (artists != null) {
            if (artists.isNotEmpty()) source.append(" ")
            val artistString = artists.joinToString(", ", transform = { artist ->
                artistNames.add(artist.name)
                artist.name
            })
            source.append(artistString)
        }
    }

    fun loadSpotifyPlaylist(context: CommandContext, tracks: Array<PlaylistTrack>) = runBlocking {
        if (tracks.size + context.getGuildMusicPlayer().guildTrackManager.tracks.size > QUEUE_LIMIT) {
            val msg = i18n.getTranslation(context, "$root.queuelimit")
                .replace("%amount%", QUEUE_LIMIT.toString())
            sendMsg(context, msg)
            return@runBlocking
        }

        val loadedTracks = mutableListOf<Track>()
        val failedTracks = mutableListOf<Track>()
        val msg = i18n.getTranslation(context, "command.play.loadingtrack" + if (tracks.size > 1) "s" else "")
            .replace("%songCount%", tracks.size.toString())
            .replace("%donateAmount%", DONATE_QUEUE_LIMIT.toString())
        val message = sendMsg(context, msg)
        for (track in tracks.map { playListTrack -> playListTrack.track }) {
            loadSpotifyTrack(context, YT_SELECTOR + track.name, track.artists, track.durationMs, true) {
                if (it) {
                    loadedTracks.add(track)
                } else {
                    failedTracks.add(track)
                }
                if (loadedTracks.size + failedTracks.size == tracks.size) {
                    runBlocking {
                        val newMsg = i18n.getTranslation(context, "command.play.loadedtrack" + if (tracks.size > 1) "s" else "")
                            .replace("%loadedCount%", loadedTracks.size.toString())
                            .replace("%failedCount%", failedTracks.size.toString())
                        message[0].editMessage(newMsg).await()
                    }
                }
            }
        }
    }

    fun loadSpotifyAlbum(context: CommandContext, simpleTracks: Array<TrackSimplified>) = runBlocking {
        if (simpleTracks.size + context.getGuildMusicPlayer().guildTrackManager.tracks.size > QUEUE_LIMIT) {
            val msg = i18n.getTranslation(context, "$root.queuelimit")
                .replace("%amount%", QUEUE_LIMIT.toString())
                .replace("%donateAmount%", DONATE_QUEUE_LIMIT.toString())
            sendMsg(context, msg)
            return@runBlocking
        }

        val loadedTracks = mutableListOf<TrackSimplified>()
        val failedTracks = mutableListOf<TrackSimplified>()
        val msg = i18n.getTranslation(context, "command.play.loadingtrack" + if (simpleTracks.size > 1) "s" else "")
            .replace("%trackCount%", simpleTracks.size.toString())
        val message = sendMsg(context, msg)
        for (track in simpleTracks) {
            loadSpotifyTrack(context, YT_SELECTOR + track.name, track.artists, track.durationMs, true) {
                if (it) {
                    loadedTracks.add(track)
                } else {
                    failedTracks.add(track)
                }
                if (loadedTracks.size + failedTracks.size == simpleTracks.size) {
                    runBlocking {
                        val newMsg = i18n.getTranslation(context, "command.play.loadedtrack" + if (simpleTracks.size > 1) "s" else "")
                            .replace("%loadedCount%", loadedTracks.size.toString())
                            .replace("%failedCount%", failedTracks.size.toString())
                        message[0].editMessage(newMsg).await()
                    }
                }
            }
        }
    }
}