package me.melijn.melijnbot.objects.utils

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchListResponse
import com.google.api.services.youtube.model.SearchResult
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.SearchType
import net.dv8tion.jda.api.entities.Guild
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
        videoCallback: (videoId: String?) -> Unit,
        audioTrackCallBack: (audioTrack: List<AudioTrack>) -> Unit,
        llDisabledAndNotYT: () -> Unit,
        lpCallback: AudioLoadResultHandler
    ) = youtubeService.launch {
        val lManager = Container.instance.lavaManager
        if (lManager.lavalinkEnabled) {
            val restClient = lManager.jdaLavaLink?.getLink(guild)?.getNode(true)?.restClient
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
                val search: YouTube.Search.List = youtube.search().list("id")

                // Set your developer key from the {{ Google Cloud Console }} for
                // non-authenticated requests. See:
                // {{ https://cloud.google.com/console }}
                val apiKey: String = properties.getProperty("youtube.apikey")
                search.key = apiKey
                search.q = query

                // Restrict the search results to only include videos. See:
                // https://developers.google.com/youtube/v3/docs/search/list#type
                search.type = "video"

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