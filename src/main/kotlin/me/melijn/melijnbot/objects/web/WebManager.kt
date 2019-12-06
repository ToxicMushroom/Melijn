package me.melijn.melijnbot.objects.web


import com.neovisionaries.i18n.CountryCode
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.exceptions.SpotifyWebApiException
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
import me.melijn.melijnbot.objects.translation.*
import me.melijn.melijnbot.objects.utils.toLCC
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import ru.gildor.coroutines.okhttp.await
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val spotifyTrackUrl: Pattern = Pattern.compile("https://open.spotify.com/track/(\\S+)")
private val spotifyTrackUri: Pattern = Pattern.compile("spotify:track:(\\S+)")
private val spotifyPlaylistUrl: Pattern = Pattern.compile("https://open.spotify.com(?:/user/\\S+)?/playlist/(\\S+)")
private val spotifyPlaylistUri: Pattern = Pattern.compile("spotify:(?:user:\\S+:)?playlist:(\\S+)")
private val spotifyAlbumUrl: Pattern = Pattern.compile("https://open.spotify.com/album/(\\S+)")
private val spotifyAlbumUri: Pattern = Pattern.compile("spotify:album:(\\S+)")
private val spotifyArtistUrl: Pattern = Pattern.compile("https://open.spotify.com/artist/(\\S+)")
private val spotifyArtistUri: Pattern = Pattern.compile("spotify:artist:(\\S+)")

class WebManager(val taskManager: TaskManager, val settings: Settings) {

    val logger = LoggerFactory.getLogger(WebManager::class.java.name)
val jsonMedia = "application/json".toMediaType()

    private val httpClient = OkHttpClient()
        .newBuilder()
        .dispatcher(Dispatcher(taskManager.executorService))
        .build()

    private lateinit var spotifyApi: SpotifyApi
    private val weebApi: WeebApi = WeebApiBuilder(TokenType.WOLKETOKENS)
        .setBotInfo(settings.name, settings.version, settings.environment.toLCC())
        .setToken(settings.tokens.weebSh)
        .build()

    init {
        updateSpotifyCredentials()
    }

    fun updateSpotifyCredentials() {
        spotifyApi = SpotifyApi.Builder()
            .setClientId(settings.spotify.clientId)
            .setClientSecret(settings.spotify.password)
            .build()

        val credentialsRequest = spotifyApi.clientCredentials().build()
        CoroutineScope(Dispatchers.IO).launch {
            spotifyApi.accessToken = credentialsRequest.executeAsync().await().accessToken
        }
    }

    suspend fun getJsonFromUrl(url: String, parameters: Map<String, String> = emptyMap()): DataObject? = suspendCoroutine {
        taskManager.async {
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
                response.close()
                return@async
            }
            withContext(Dispatchers.IO) {
                val responseString = responseBody.string()
                it.resume(DataObject.fromJson(responseString))
            }
            response.close()
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
        trackList: (Array<Track>) -> Unit,
        simpleTrack: (Array<TrackSimplified>) -> Unit,
        error: (Throwable) -> Unit
    ) = taskManager.async {
        try {
            //Tracks
            when {
                spotifyTrackUrl.matcher(songArg).matches() -> track.invoke(spotifyApi.getTrack(songArg.replaceFirst("https://open.spotify.com/track/".toRegex(), "").replaceFirst("\\?\\S+".toRegex(), "")).build().execute())
                spotifyTrackUri.matcher(songArg).matches() -> track.invoke(spotifyApi.getTrack(songArg.replaceFirst("spotify:track:".toRegex(), "").replaceFirst("\\?\\S+".toRegex(), "")).build().execute())

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

    private suspend fun fetchTracksFromArtist(songArg: String, trackList: (Array<Track>) -> Unit, spotifyArtistUrl: Pattern) {
        val matcher: Matcher = spotifyArtistUrl.matcher(songArg)
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                val id = matcher.group(1).replaceFirst("\\?\\S+".toRegex(), "")
                val tracks = spotifyApi.getArtistsTopTracks(id, CountryCode.US).build().executeAsync().await()
                trackList(tracks)
            }
        }
    }


    private suspend fun acceptTracksIfMatchesPattern(url: String, trackList: (Array<Track>) -> Unit, pattern: Pattern) {
        val matcher: Matcher = pattern.matcher(url)
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                val id = matcher.group(1).replaceFirst("\\?\\S+".toRegex(), "")
                val tracks = spotifyApi.getPlaylistsTracks(id).build().executeAsync().await().items.map { playlistTrack ->
                    playlistTrack.track
                }
                trackList(tracks.toTypedArray())
            }
        }
    }

    private suspend fun acceptIfMatchesPattern(url: String, simpleTrack: (Array<TrackSimplified>) -> Unit, pattern: Pattern) {
        val matcher: Matcher = pattern.matcher(url)
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                val id = matcher.group(1).replaceFirst("\\?\\S+".toRegex(), "")
                val simpleTracks = spotifyApi.getAlbumsTracks(id).build().executeAsync().await().items
                simpleTrack(simpleTracks)
            }
        }
    }

    private val defaultCallbackHandler = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            logger.error(e.message ?: return)
        }

        override fun onResponse(call: Call, response: Response) {
            response.close()
        }
    }

    fun updateTopDotGG(serversArray: List<Long>) {
        val token = settings.tokens.topDotGG
        val url = "$TOP_GG_URL/api/bots/${settings.id}/stats"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("shards", serversArray.joinToString(",", "[", "]"))
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateBotsOnDiscordXYZ(servers: Long) {
        val token = settings.tokens.botsOnDiscordXYZ
        val url = "$BOTS_ON_DISCORD_XYZ_URL/bot-api/bots/${settings.id}/guilds"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("guildCount", "$servers")
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateBotlistSpace(serversArray: List<Long>) {
        val token = settings.tokens.botlistSpace
        val url = "$BOTLIST_SPACE/v1/bots/${settings.id}"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("shards", serversArray.joinToString(",", "[", "]"))
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateDiscordBotListCom(servers: Long, users: Long) {
        val token = settings.tokens.discordBotListCom
        val url = "$DISCORD_BOT_LIST_COM/api/bots/${settings.id}/stats"
        if (token.isBlank()) return
        taskManager.async {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("guilds", "$servers")
                .addFormDataPart("users", "$users")
                .build()

            val request = Request.Builder()
                .addHeader("Authorization", "Bot $token")
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateDivinedDiscordBots(servers: Long, shards: Long) {
        val token = settings.tokens.divineDiscordBotsCom
        val url = "$DIVINE_DISCORD_BOTS_COM/bot/${settings.id}/stats"
        if (token.isBlank()) return

        taskManager.async {
            val body = DataObject.empty()
                .put("server_count", servers)
                .put("shard_count", shards)
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateDiscordBotsGG(servers: Long, shards: Long) {
        val token = settings.tokens.discordBotsGG
        val url = "$DISCORD_BOTS_GG/bots/${settings.id}/stats"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("guildCount", servers)
                .put("shardCount", shards)
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateBotsForDiscordCom(servers: Long) {
        val token = settings.tokens.botsForDiscordCom
        val url = "$BOTS_FOR_DISCORD_COM/api/bot/${settings.id}"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("server_count", servers)
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateDiscordBoats(servers: Long) {
        val token = settings.tokens.discordBoats
        val url = "$DISCORD_BOATS/api/bot/${settings.id}"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("server_count", servers)
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }
}