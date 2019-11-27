package me.melijn.melijnbot.database.audio

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import lavalink.client.LavalinkUtil
import java.util.*

class TracksWrapper(val tracksDao: TracksDao, val lastVoiceChannelDao: LastVoiceChannelDao) {

    suspend fun getMap(): Map<Long, List<AudioTrack>> {
        val map = tracksDao.getMap()
        val newMap = mutableMapOf<Long, List<AudioTrack>>()

        for ((guildId, list) in map) {
            val newList = mutableListOf<AudioTrack>()
            for (string in list.sortedBy { (first) -> first }) {
                newList.add(LavalinkUtil.toAudioTrack(string.second))
            }
            newMap[guildId] = newList
        }

        return newMap
    }

    suspend fun put(guildId: Long, playingTrack: AudioTrack, queue: Queue<AudioTrack>) {
        val playing = LavalinkUtil.toMessage(playingTrack)
        tracksDao.set(guildId, 0, playing)

        for ((index, track) in queue.withIndex()) {
            val json = LavalinkUtil.toMessage(track)
            tracksDao.set(guildId, index + 1, json)
        }
    }

    suspend fun clear() {
        tracksDao.clear()
    }

    suspend fun addChannel(guildId: Long, channelId: Long) {
        lastVoiceChannelDao.add(guildId, channelId)
    }

    suspend fun getChannels(): Map<Long, Long> {
        return lastVoiceChannelDao.getMap()
    }

    suspend fun clearChannels() {
        lastVoiceChannelDao.clear()
    }
}