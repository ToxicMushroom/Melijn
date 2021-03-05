package me.melijn.melijnbot.internals.web.spotify

import com.neovisionaries.i18n.CountryCode
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.exceptions.SpotifyWebApiException
import com.wrapper.spotify.model_objects.specification.Track
import com.wrapper.spotify.model_objects.specification.TrackSimplified
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.threading.TaskManager
import java.io.IOException
import kotlin.math.min

class MySpotifyApi(spotifySettings: Settings.Api.Spotify) {

    private var spotifyApi: SpotifyApi = SpotifyApi.Builder()
        .setClientId(spotifySettings.clientId)
        .setClientSecret(spotifySettings.password)
        .build()

    init {
        updateSpotifyCredentials()
    }

    fun updateSpotifyCredentials() {
        val credentialsRequest = spotifyApi.clientCredentials().build()
        TaskManager.async {
            spotifyApi.accessToken = credentialsRequest.executeAsync().await().accessToken
        }
    }


    companion object {
        private val spotifyTrackUrl = Regex("https://open\\.spotify\\.com/track/(\\w+)(?:\\?\\S+)?")
        private val spotifyTrackUri = Regex("spotify:track:(\\w+)")
        private val spotifyPlaylistUrl = Regex("https://open\\.spotify\\.com(?:/user/.*)?/playlist/(\\w+)(?:\\?\\S+)?")
        private val spotifyPlaylistUri = Regex("spotify:(?:user:\\S+:)?playlist:(\\w+)")
        private val spotifyAlbumUrl = Regex("https://open\\.spotify\\.com/album/(\\w+)(?:\\?\\S+)?")
        private val spotifyAlbumUri = Regex("spotify:album:(\\w+)")
        private val spotifyArtistUrl = Regex("https://open\\.spotify\\.com/artist/(\\w+)(?:\\?\\S+)?")
        private val spotifyArtistUri = Regex("spotify:artist:(\\w+)")
    }


    // TODO provide info about current queue size and size limit -> get the correct amount of spotify tracks
    // TODO return if all tracks were obtained from the playlist or not -> send message if not that the queue is now full
    fun getTracksFromSpotifyUrl(
        songArg: String,
        track: suspend (Track) -> Unit,
        trackList: suspend (Array<Track>) -> Unit,
        simpleTrack: suspend (Array<TrackSimplified>) -> Unit,
        error: suspend (Throwable) -> Unit
    ) = TaskManager.async {
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
                else -> throw IllegalArgumentException("That is not a valid spotify link")
            }
        } catch (ignored: IOException) {
            error.invoke(ignored)
        } catch (ignored: SpotifyWebApiException) {
            error.invoke(ignored)
        } catch (ignored: IllegalArgumentException) {
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

    private suspend fun acceptPlaylistResults(
        songArg: String,
        trackList: suspend (Array<Track>) -> Unit,
        regex: Regex
    ) {
        val result = requireNotNull(regex.find(songArg)) { "bruh" }
        val id = result.groupValues[1]
        val paginTracks = spotifyApi
            .getPlaylistsItems(id)
            .limit(100)
            .build()
            .executeAsync()
            .await()
        val tracks = mutableListOf<Track>()
        tracks.addAll(
            paginTracks.items
                .mapNotNull { playlistTrack ->
                    (playlistTrack.track as Track?)
                }
        )
        val trackTotal = min(paginTracks.total, 1000)
        var tracksGottenOffset = 100
        while (trackTotal > tracksGottenOffset) {

            val moreTracks = spotifyApi.getPlaylistsItems(id).limit(100).offset(tracksGottenOffset)
                .build().executeAsync().await().items

            tracksGottenOffset += moreTracks.size
            tracks.addAll(
                moreTracks.mapNotNull { playlistTrack ->
                    (playlistTrack.track as Track?)
                }
            )
        }


        trackList(tracks.toTypedArray())
    }

    private suspend fun acceptAlbumResults(
        songArg: String,
        simpleTrack: suspend (Array<TrackSimplified>) -> Unit,
        regex: Regex
    ) {
        val result = requireNotNull(regex.find(songArg)) { "bruh" }
        val id = result.groupValues[1]
        val simpleTracks = spotifyApi.getAlbumsTracks(id).build().executeAsync().await().items

        simpleTrack(simpleTracks)
    }

}