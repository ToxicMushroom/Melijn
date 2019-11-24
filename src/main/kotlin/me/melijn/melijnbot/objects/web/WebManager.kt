package me.melijn.melijnbot.objects.web


import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.exceptions.SpotifyWebApiException
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
import me.melijn.melijnbot.objects.translation.*
import me.melijn.melijnbot.objects.utils.toLCC
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
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
                return@async
            }
            withContext(Dispatchers.IO) {
                val responseString = responseBody.string()
                it.resume(DataObject.fromJson(responseString))
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
                else -> error.invoke(IllegalArgumentException("That is not a valid spotify link"))
            }
        } catch (ignored: IOException) {
            error.invoke(ignored)
        } catch (ignored: SpotifyWebApiException) {
            error.invoke(ignored)
        }
    }


    private suspend fun acceptTracksIfMatchesPattern(url: String, trackList: (Array<PlaylistTrack>) -> Unit, pattern: Pattern) {
        val matcher: Matcher = pattern.matcher(url)
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                val id = matcher.group(1).replaceFirst("\\?\\S+".toRegex(), "")
                val tracks = spotifyApi.getPlaylistsTracks(id).build().executeAsync().await().items
                trackList(tracks)
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

    fun updateTopDotGG(serversArray: List<Long>) {
        val token = settings.tokens.topDotGG
        val url = "$TOP_GG_URL/bots/${settings.id}/stats"
        if (token.isBlank()) return
        taskManager.async {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("shards", serversArray.joinToString(",", "[", "]"))
                .build()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).await().close()
        }
    }

    fun updateBotsOnDiscordXYZ(servers: Long) {
        val token = settings.tokens.botsOnDiscordXYZ
        val url = "$BOTS_ON_DISCORD_XYZ_URL/bot-api/bots/${settings.id}/guilds"
        if (token.isBlank()) return
        taskManager.async {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("guildCount", "$servers")
                .build()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).await().close()
        }
    }

    fun updateBotlistSpace(serversArray: List<Long>) {
        val token = settings.tokens.botlistSpace
        val url = "$BOTLIST_SPACE/v1/bots/${settings.id}"
        if (token.isBlank()) return
        taskManager.async {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("shards", serversArray.joinToString(",", "[", "]"))
                .build()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).await().close()
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

            httpClient.newCall(request).await().close()
        }
    }

    fun updateDivinedDiscordBots(servers: Long, shards: Long) {
        val token = settings.tokens.divinedDiscordBotsCom
        val url = "$DIVINED_DISCORD_BOTS_COM/bot/${settings.id}/stats"
        if (token.isBlank()) return
        taskManager.async {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("server_count", "$servers")
                .addFormDataPart("shard_count", "$shards")
                .build()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).await().close()
        }
    }

    fun updateDiscordBotsGG(servers: Long, shards: Long) {
        val token = settings.tokens.discordBotsGG
        val url = "$DISCORD_BOTS_GG/bots/${settings.id}/stats"
        if (token.isBlank()) return
        taskManager.async {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("guildCount", "$servers")
                .addFormDataPart("shardCount", "$shards")
                .build()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).await().close()
        }
    }

    fun updateBotsForDiscordCom(servers: Long) {
        val token = settings.tokens.botsForDiscordCom
        val url = "$BOTS_FOR_DISCORD_COM/api/bot/${settings.id}"
        if (token.isBlank()) return
        taskManager.async {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("server_count", "$servers")
                .build()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).await().close()
        }
    }

    fun updateDiscordBoats(servers: Long) {
        val token = settings.tokens.discordBoats
        val url = "$DISCORD_BOATS/api/bot/${settings.id}"
        if (token.isBlank()) return
        taskManager.async {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("server_count", "$servers")
                .build()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).await().close()
        }
    }
}