package me.melijn.melijnbot.objects.web.spotify

import com.neovisionaries.i18n.CountryCode
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.exceptions.SpotifyWebApiException
import com.wrapper.spotify.model_objects.specification.Track
import com.wrapper.spotify.model_objects.specification.TrackSimplified
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.removeFirst
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

class MySpotifyApi(val taskManager: TaskManager, spotifySettings: Settings.Spotify) {

    private var spotifyApi: SpotifyApi = SpotifyApi.Builder()
        .setClientId(spotifySettings.clientId)
        .setClientSecret(spotifySettings.password)
        .build()

    init {
        updateSpotifyCredentials()
    }

    fun updateSpotifyCredentials() {
        val credentialsRequest = spotifyApi.clientCredentials().build()
        CoroutineScope(Dispatchers.IO).launch {
            spotifyApi.accessToken = credentialsRequest.executeAsync().await().accessToken
        }
    }


    companion object {
        private val spotifyTrackUrl: Pattern = Pattern.compile("https://open\\.spotify\\.com/track/(\\S+)")
        private val spotifyTrackUri: Pattern = Pattern.compile("spotify:track:(\\S+)")
        private val spotifyPlaylistUrl: Pattern = Pattern.compile("https://open\\.spotify\\.com(?:/user/\\S+)?/playlist/(\\S+)")
        private val spotifyPlaylistUri: Pattern = Pattern.compile("spotify:(?:user:\\S+:)?playlist:(\\S+)")
        private val spotifyAlbumUrl: Pattern = Pattern.compile("https://open\\.spotify\\.com/album/(\\S+)")
        private val spotifyAlbumUri: Pattern = Pattern.compile("spotify:album:(\\S+)")
        private val spotifyArtistUrl: Pattern = Pattern.compile("https://open\\.spotify\\.com/artist/(\\S+)")
        private val spotifyArtistUri: Pattern = Pattern.compile("spotify:artist:(\\S+)")
    }

    fun getTracksFromSpotifyUrl(
        songArg: String,
        track: suspend (Track) -> Unit,
        trackList: suspend (Array<Track>) -> Unit,
        simpleTrack: suspend (Array<TrackSimplified>) -> Unit,
        error: suspend (Throwable) -> Unit
    ) = taskManager.async {
        try {
            //Tracks
            when {
                spotifyTrackUrl.matcher(songArg).matches() -> {
                    val trackId = songArg
                        .removeFirst("https://open.spotify.com/track/")
                        .removeFirst("\\?\\S+".toRegex())
                    track.invoke(spotifyApi.getTrack(trackId).build().executeAsync().await())
                }
                spotifyTrackUri.matcher(songArg).matches() -> {
                    val trackId = songArg
                        .removeFirst("spotify:track:")
                        .removeFirst("\\?\\S+".toRegex())
                    track.invoke(spotifyApi.getTrack(trackId).build().executeAsync().await())
                }

                //Playlists
                spotifyPlaylistUrl.matcher(songArg).matches() -> acceptTracksIfMatchesPattern(songArg, trackList, spotifyPlaylistUrl)
                spotifyPlaylistUri.matcher(songArg).matches() -> acceptTracksIfMatchesPattern(songArg, trackList, spotifyPlaylistUri)

                //Albums
                spotifyAlbumUrl.matcher(songArg).matches() -> acceptIfMatchesPattern(songArg, simpleTrack, spotifyAlbumUrl)
                spotifyAlbumUri.matcher(songArg).matches() -> acceptIfMatchesPattern(songArg, simpleTrack, spotifyAlbumUri)

                spotifyArtistUrl.matcher(songArg).matches() -> fetchTracksFromArtist(songArg, trackList, spotifyArtistUrl)
                spotifyArtistUri.matcher(songArg).matches() -> fetchTracksFromArtist(songArg, trackList, spotifyArtistUri)
                else -> error.invoke(IllegalArgumentException("That is not a valid spotify link"))
            }
        } catch (ignored: IOException) {
            error.invoke(ignored)
        } catch (ignored: SpotifyWebApiException) {
            error.invoke(ignored)
        }
    }

    private suspend fun fetchTracksFromArtist(songArg: String, trackList: suspend(Array<Track>) -> Unit, spotifyArtistUrl: Pattern) {
        val matcher: Matcher = spotifyArtistUrl.matcher(songArg)
        while (matcher.find()) {
            if (matcher.group(1) == null) continue

            val id = matcher.group(1).removeFirst("\\?\\S+".toRegex())
            val tracks = spotifyApi.getArtistsTopTracks(id, CountryCode.US).build().executeAsync().await()
            trackList(tracks)
        }
    }


    private suspend fun acceptTracksIfMatchesPattern(url: String, trackList: suspend (Array<Track>) -> Unit, pattern: Pattern) {
        val matcher: Matcher = pattern.matcher(url)
        while (matcher.find()) {
            if (matcher.group(1) == null) continue

            val id = matcher.group(1).removeFirst("\\?\\S+".toRegex())
            val tracks = spotifyApi.getPlaylistsItems(id).build().executeAsync().await().items.map { playlistTrack ->
                (playlistTrack.track as Track)
            }

            trackList(tracks.toTypedArray())
        }
    }

    private suspend fun acceptIfMatchesPattern(url: String, simpleTrack: suspend (Array<TrackSimplified>) -> Unit, pattern: Pattern) {
        val matcher: Matcher = pattern.matcher(url)
        while (matcher.find()) {
            if (matcher.group(1) == null) continue

            val id = matcher.group(1).removeFirst("\\?\\S+".toRegex())
            val simpleTracks = spotifyApi.getAlbumsTracks(id).build().executeAsync().await().items
            simpleTrack(simpleTracks)
        }
    }

}