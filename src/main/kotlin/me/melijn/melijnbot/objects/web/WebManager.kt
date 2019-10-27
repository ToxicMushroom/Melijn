package me.melijn.melijnbot.objects.web


import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.specification.PlaylistTrack
import com.wrapper.spotify.model_objects.specification.Track
import com.wrapper.spotify.model_objects.specification.TrackSimplified
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.duncte123.weebJava.WeebApiBuilder
import me.duncte123.weebJava.models.WeebApi
import me.duncte123.weebJava.types.TokenType
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.objects.utils.toLCC
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val spotifyTrackUrl: Pattern = Pattern.compile("https://open.spotify.com/track/(\\S+)")
private val spotifyTrackUri: Pattern = Pattern.compile("spotify:track:(\\S+)")
private val spotifyPlaylistUrl: Pattern = Pattern.compile("https://open.spotify.com(?:/user/\\S+)?/playlist/(\\S+)")
private val spotifyPlaylistUri: Pattern = Pattern.compile("spotify:(?:user:\\S+:)?playlist:(\\S+)")
private val spotifyAlbumUrl: Pattern = Pattern.compile("https://open.spotify.com/album/(\\S+)")
private val spotifyAlbumUri: Pattern = Pattern.compile("spotify:album:(\\S+)")

class WebManager(val taskManager: TaskManager, val settings: Settings) {

    private val httpClient = OkHttpClient()
    private lateinit var spotifyApi: SpotifyApi
    private val weebApi: WeebApi = WeebApiBuilder(TokenType.WOLKETOKENS)
        .setBotInfo(settings.name, settings.version, settings.environment.toLCC())
        .setToken(settings.tokens.weebSh)
        .build()

    init {
        updateSpotifyCredentials()
    }

    //TODO ("Add service that calls this function each 1_800_000 millis")
    private fun updateSpotifyCredentials() {
        spotifyApi = SpotifyApi.Builder()
            .setClientId(settings.spotify.clientId)
            .setClientSecret(settings.spotify.password)
            .build()

        val credentialsRequest = spotifyApi.clientCredentials().build()
        CoroutineScope(Dispatchers.IO).launch {
            spotifyApi.accessToken = credentialsRequest.executeAsync().await().accessToken
        }
    }

    suspend fun getJsonFromUrl(url: String, parameters: Map<String, String> = emptyMap()): JsonNode? = suspendCoroutine {
        taskManager.async {
            val mapper = ObjectMapper()
            val fullUrlWithParams = url + parameters.entries.joinToString("&", "?",
                transform = { entry ->
                    entry.key + "=" + entry.value
                }
            )
            val request = Request.Builder()
                .url(fullUrlWithParams)
                .get()
                .build()

            val response = httpClient.newCall(request).await()
            val responseBody = response.body
            if (responseBody == null) {
                it.resume(null)
                return@async
            }
            withContext(Dispatchers.IO) {
                val responseString = responseBody.string()
                it.resume(mapper.readTree(responseString))
            }
        }
    }

    suspend fun getWeebJavaUrl(type: String): String = suspendCoroutine {
        weebApi.getRandomImage(type).async({ image ->
            it.resume(image.url)
        }, { _ ->
            it.resume(MISSING_IMAGE_URL)
        })
    }

    suspend fun getWeebTypes(): String = suspendCoroutine {
        weebApi.types.async({ types ->
            it.resume(types.types.joinToString())
        }, { _ ->
            it.resume("error")
        })
    }

    fun getTracksFromSpotifyUrl(
        songArg: String,
        track: (Track) -> Unit,
        trackList: (Array<PlaylistTrack>) -> Unit,
        simpleTrack: (Array<TrackSimplified>) -> Unit,
        error: (Throwable) -> Unit
    ) = taskManager.async {

    }
}