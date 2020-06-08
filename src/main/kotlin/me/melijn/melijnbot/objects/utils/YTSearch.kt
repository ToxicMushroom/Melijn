package me.melijn.melijnbot.objects.utils

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchListResponse
import com.google.api.services.youtube.model.SearchResult
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import kotlinx.coroutines.future.await
import lavalink.client.LavalinkUtil
import lavalink.client.io.LavalinkRestClient
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.SearchType
import me.melijn.melijnbot.objects.music.SuspendingAudioLoadResultHandler
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.utils.data.DataObject
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class YTSearch {

    private val youtube: YouTube
    private val properties: Properties = Properties()
    private val youtubeService: ExecutorService = Executors.newCachedThreadPool { r: Runnable? -> Thread(r, "Youtube-Search-Thread") }
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    init {
        val propertiesFileName = "youtube.properties"
        try {
            val inputStream = YouTube.Search::class.java.getResourceAsStream("/$propertiesFileName")
            properties.load(inputStream)
        } catch (e: IOException) {
            logger.error("There was an error reading $propertiesFileName: ${e.cause} : ${e.message}")
        }
        val jsonFactory = JacksonFactory()
        //https://github.com/youtube/api-samples/blob/master/java/src/main/java/com/google/api/services/samples/youtube/cmdline/data/Search.java


        val httpTransport: HttpTransport = NetHttpTransport()
        youtube = YouTube.Builder(
            httpTransport,
            jsonFactory,
            HttpRequestInitializer {}
        ).setApplicationName("youtube-search").build()
    }


    fun search(
        guild: Guild, query: String, searchType: SearchType,
        videoCallback: suspend (videoId: String?) -> Unit,
        audioTrackCallBack: suspend (audioTrack: List<AudioTrack>) -> Unit,
        llDisabledAndNotYT: suspend () -> Unit,
        lpCallback: SuspendingAudioLoadResultHandler
    ) = youtubeService.launch {
        val lManager = Container.instance.lavaManager
        if (lManager.lavalinkEnabled) {
            val prem = Container.instance.daoManager.musicNodeWrapper.isPremium(guild.idLong)
            val llink = if (prem && lManager.premiumLavaLink != null) {
                lManager.premiumLavaLink
            } else {
                lManager.jdaLavaLink
            }

            val restClient = llink?.getLink(guild)?.getNode(true)?.restClient
            val tracks = when (searchType) {
                SearchType.SC -> {
                    restClient?.getSoundCloudSearchResult(query)?.await()
                }
                SearchType.YT -> {
                    restClient?.getYoutubeSearchResult(query)?.await()
                }
                else -> {
                    restClient?.loadItem(query, lpCallback)
                    return@launch
                }
            }
            if (tracks != null) {
                audioTrackCallBack(tracks)
                return@launch
            }
        }

        if (searchType == SearchType.YT) {
            try {
                val search: YouTube.Search.List = youtube.search().list(listOf("id"))

                // Set your developer key from the {{ Google Cloud Console }} for
                // non-authenticated requests. See:
                // {{ https://cloud.google.com/console }}
                val apiKey: String = properties.getProperty("youtube.apikey")
                search.key = apiKey
                search.q = query

                // Restrict the search results to only include videos. See:
                // https://developers.google.com/youtube/v3/docs/search/list#type
                search.type = listOf("video")

                // To increase efficiency, only retrieve the fields that the
                // application uses.
                search.fields = "items(id/videoId)"
                search.maxResults = 1L

                // Call the API adn return results
                val searchResponse: SearchListResponse = search.execute()
                val searchResultList: List<SearchResult> = searchResponse.items
                if (searchResultList.isEmpty()) {
                    videoCallback.invoke(null)
                    return@launch
                }

                val id: String = searchResultList[0].id.videoId
                videoCallback.invoke(id)
                return@launch
            } catch (e: GoogleJsonResponseException) {
                logger.error("There was a service error: ${e.details.code} : ${e.details.message}")
                e.sendInGuild()
            } catch (e: IOException) {
                logger.error("There was an IO error: ${e.cause} : ${e.message}")
                e.sendInGuild()
            } catch (t: Throwable) {
                t.sendInGuild()
                t.printStackTrace()
            }
            videoCallback.invoke(null)
            return@launch
        }

        llDisabledAndNotYT()
    }
}

private suspend fun LavalinkRestClient.loadItem(query: String, lpCallback: SuspendingAudioLoadResultHandler) {
    consumeCallback(lpCallback).invoke(load(query).await())
}

suspend fun consumeCallback(callback: SuspendingAudioLoadResultHandler): suspend (DataObject?) -> Unit {
    return label@{ loadResult: DataObject? ->
        if (loadResult == null) {
            callback.noMatches()
            return@label
        }
        try {
            val loadType = loadResult.getString("loadType")
            when (loadType) {
                "TRACK_LOADED" -> {
                    val trackDataSingle = loadResult.getArray("tracks")
                    val trackObject = trackDataSingle.getObject(0)
                    val singleTrackBase64 = trackObject.getString("track")
                    val singleAudioTrack = LavalinkUtil.toAudioTrack(singleTrackBase64)
                    callback.trackLoaded(singleAudioTrack)
                }
                "PLAYLIST_LOADED" -> {
                    val trackData = loadResult.getArray("tracks")
                    val tracks: MutableList<AudioTrack> = ArrayList()
                    var index = 0
                    while (index < trackData.length()) {
                        val track = trackData.getObject(index)
                        val trackBase64 = track.getString("track")
                        val audioTrack = LavalinkUtil.toAudioTrack(trackBase64)
                        tracks.add(audioTrack)
                        index++
                    }
                    val playlistInfo = loadResult.getObject("playlistInfo")
                    val selectedTrackId = playlistInfo.getInt("selectedTrack")
                    val selectedTrack: AudioTrack
                    selectedTrack = if (selectedTrackId < tracks.size && selectedTrackId >= 0) {
                        tracks[selectedTrackId]
                    } else {
                        if (tracks.size == 0) {
                            callback.loadFailed(FriendlyException(
                                "Playlist is empty",
                                FriendlyException.Severity.SUSPICIOUS,
                                IllegalStateException("Empty playlist")
                            ))
                            return@label
                        }
                        tracks[0]
                    }
                    val playlistName = playlistInfo.getString("name")
                    val playlist = BasicAudioPlaylist(playlistName, tracks, selectedTrack, true)
                    callback.playlistLoaded(playlist)
                }
                "NO_MATCHES" -> callback.noMatches()
                "LOAD_FAILED" -> {
                    val exception = loadResult.getObject("exception")
                    val message = exception.getString("message")
                    val severity = FriendlyException.Severity.valueOf(exception.getString("severity"))
                    val friendlyException = FriendlyException(message, severity, Throwable())
                    callback.loadFailed(friendlyException)
                }
                else -> throw IllegalArgumentException("Invalid loadType: $loadType")
            }
        } catch (ex: Exception) {
            callback.loadFailed(FriendlyException(ex.message, FriendlyException.Severity.FAULT, ex))
        }
    }
}