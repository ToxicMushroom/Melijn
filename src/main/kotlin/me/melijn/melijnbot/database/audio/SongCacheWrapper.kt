package me.melijn.melijnbot.database.audio

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.llklient.utils.LavalinkUtil

class SongCacheWrapper(private val songCacheDao: SongCacheDao) {

    suspend fun getTrackInfo(song: String): AudioTrack? {
        return songCacheDao.getCacheEntry(song, 10080)?.let {
            LavalinkUtil.toAudioTrack(it)
        }
    }

    fun addTrack(song: String, track: AudioTrack) {
        val trackInfo = LavalinkUtil.toMessage(track)
        songCacheDao.setCacheEntry(song, trackInfo, 4320)
    }
}