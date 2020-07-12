package me.melijn.melijnbot.internals.utils

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist
import kotlinx.coroutines.future.await
import me.melijn.llklient.io.LavalinkRestClient
import me.melijn.llklient.utils.LavalinkUtil

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.SearchType
import me.melijn.melijnbot.internals.music.SuspendingAudioLoadResultHandler
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.utils.data.DataObject
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val http = Regex("^(?:(?:https?)://)(?:\\S+(?::\\S*)?@)?(?:(?!10(?:\\.\\d{1,3}){3})(?!127(?:\\.\\d{1,3}){3})(?!169\\.254(?:\\.\\d{1,3}){2})(?!192\\.168(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}0-9]+-?)*[a-z\\x{00a1}-\\x{ffff}0-9]+)*(?:\\.(?:[a-z\\x{00a1}-\\x{ffff}]{2,})))(?::\\d{2,5})?(?:/[^\\s]*)?$")

class YTSearch {

    companion object {
        fun isUnknownHTTP(query: String): Boolean {
            return if (http.matches(query)) {
                !(query.startsWith("https://twitch.tv/", true) ||
                    query.startsWith("https://wwww.youtube.com/", true) ||
                    query.startsWith("https://youtube.com/", true) ||
                    query.startsWith("https://youtu.be/", true) ||
                    query.startsWith("https://soundcloud.com/", true) ||
                    query.startsWith("https://vimeo.com/", true) ||
                    query.startsWith("https://getyarn.io/", true))
            } else {
                false
            }
        }
    }

    private val youtubeService: ExecutorService = Executors.newCachedThreadPool { r: Runnable? -> Thread(r, "Youtube-Search-Thread") }


    fun search(
        guild: Guild, query: String, searchType: SearchType,
        audioTrackCallBack: suspend (audioTrack: List<AudioTrack>) -> Unit,
        llDisabledAndNotYT: suspend () -> Unit,
        lpCallback: SuspendingAudioLoadResultHandler
    ) = youtubeService.launch {
        val lManager = Container.instance.lavaManager
        if (lManager.lavalinkEnabled) {
            val prem = Container.instance.daoManager.musicNodeWrapper.isPremium(guild.idLong)
            val llink = lManager.jdaLavaLink

            val restClient = llink?.getLink(guild.idLong, "normal")?.getNode(true)?.restClient
            val tracks = when (searchType) {
                SearchType.SC -> {
                    restClient?.getSoundCloudSearchResult(query)?.await()
                }
                SearchType.YT -> {
                    restClient?.getYoutubeSearchResult(query)?.await()
                }
                else -> {
                    if (isUnknownHTTP(query) && prem) {
                        val httpRestClient = llink?.getLink(guild.idLong, "http")?.getNode(true)?.restClient
                        httpRestClient?.loadItem(query, lpCallback)
                    } else {
                        restClient?.loadItem(query, lpCallback)
                    }
                    return@launch
                }
            }
            if (tracks != null) {
                audioTrackCallBack(tracks)
                return@launch
            }
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
            when (val loadType = loadResult.getString("loadType")) {
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