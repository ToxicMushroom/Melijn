package me.melijn.melijnbot.internals.web.spotify

import com.neovisionaries.i18n.CountryCode
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.exceptions.SpotifyWebApiException
import com.wrapper.spotify.model_objects.specification.Track
import com.wrapper.spotify.model_objects.specification.TrackSimplified
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.threading.TaskManager
import java.io.IOException

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
        private val spotifyTrackUrl = Regex("https://open\\.spotify\\.com/track/(\\w+)(?:\\?\\S+)?")
        private val spotifyTrackUri = Regex("spotify:track:(\\w+)")
        private val spotifyPlaylistUrl = Regex("https://open\\.spotify\\.com(?:/user/\\w+)?/playlist/(\\w+)(?:\\?\\S+)?")
        private val spotifyPlaylistUri = Regex("spotify:(?:user:\\S+:)?playlist:(\\w+)")
        private val spotifyAlbumUrl = Regex("https://open\\.spotify\\.com/album/(\\w+)(?:\\?\\S+)?")
        private val spotifyAlbumUri = Regex("spotify:album:(\\w+)")
        private val spotifyArtistUrl = Regex("https://open\\.spotify\\.com/artist/(\\w+)(?:\\?\\S+)?")
        private val spotifyArtistUri = Regex("spotify:artist:(\\w+)")
    }


    fun getTracksFromSpotifyUrl(
        songArg: String,
        track: suspend (Track) -> Unit,
        trackList: suspend (Array<Track>) -> Unit,
        simpleTrack: suspend (Array<TrackSimplified>) -> Unit,
        error: suspend (Throwable) -> Unit
    ) = taskManager.async {
        try {
            when {
                spotifyTrackUrl.matches(songArg) -> acceptTrackResult(songArg, track, spotifyTrackUrl)
                spotifyTrackUri.matches(songArg) -> acceptTrackResult(songArg, track, spotifyTrackUri)

                spotifyPlaylistUrl.matches(songArg) -> acceptPlaylistResults(songArg, trackList, spotifyPlaylistUrl)
                spotifyPlaylistUri.matches(songArg) -> acceptPlaylistResults(songArg, trackList, spotifyPlaylistUri)

                spotifyAlbumUrl.matches(songArg) -> acceptAlbumResults(songArg, simpleTrack, spotifyAlbumUrl)
                spotifyAlbumUri.matches(songArg) -> acceptAlbumResults(songArg, simpleTrack, spotifyAlbumUri)

                spotifyArtistUrl.matches(songArg) -> acceptArtistResults(songArg, trackList, spotifyArtistUrl)
                spotifyArtistUri.matches(songArg) -> acceptArtistResults(songArg, trackList, spotifyArtistUri)
                else -> error.invoke(IllegalArgumentException("That is not a valid spotify link"))
            }
        } catch (ignored: IOException) {
            error.invoke(ignored)
        } catch (ignored: SpotifyWebApiException) {
            error.invoke(ignored)
        }
    }

    private suspend fun acceptTrackResult(songArg: String, track: suspend (Track) -> Unit, regex: Regex) {
        val result = requireNotNull(regex.find(songArg)) { "bruh" }
        val trackId = result.groupValues[1]

        track.invoke(spotifyApi.getTrack(trackId).build().executeAsync().await())
    }

    private suspend fun acceptArtistResults(songArg: String, trackList: suspend (Array<Track>) -> Unit, regex: Regex) {
        val result = requireNotNull(regex.find(songArg)) { "bruh" }
        val id = result.groupValues[1]

        val tracks = spotifyApi.getArtistsTopTracks(id, CountryCode.US).build().executeAsync().await()
        trackList(tracks)
    }

    private suspend fun acceptPlaylistResults(songArg: String, trackList: suspend (Array<Track>) -> Unit, regex: Regex) {
        val result = requireNotNull(regex.find(songArg)) { "bruh" }
        val id = result.groupValues[1]
        val tracks = spotifyApi.getPlaylistsItems(id).build().executeAsync().await().items.map { playlistTrack ->
            (playlistTrack.track as Track)
        }

        trackList(tracks.toTypedArray())

    }

    private suspend fun acceptAlbumResults(songArg: String, simpleTrack: suspend (Array<TrackSimplified>) -> Unit, regex: Regex) {
        val result = requireNotNull(regex.find(songArg)) { "bruh" }
        val id = result.groupValues[1]
        val simpleTracks = spotifyApi.getAlbumsTracks(id).build().executeAsync().await().items

        simpleTrack(simpleTracks)
    }

}