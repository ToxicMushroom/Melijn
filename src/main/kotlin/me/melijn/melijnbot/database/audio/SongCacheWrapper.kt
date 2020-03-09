package me.melijn.melijnbot.database.audio

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import lavalink.client.LavalinkUtil

class SongCacheWrapper(private val songCacheDao: SongCacheDao) {

    suspend fun getTrackInfo(song: String): AudioTrack? {
        val trackInfo = songCacheDao.getTrackInfo(song)
        return trackInfo?.let { LavalinkUtil.toAudioTrack(it) }
    }

    suspend fun addTrack(song: String, track: AudioTrack) {
        val trackInfo = LavalinkUtil.toMessage(track)
        songCacheDao.addTrack(song, trackInfo)
    }

    suspend fun clearOldTracks() {
        songCacheDao.clearOldTracks()
    }

}