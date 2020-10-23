package me.melijn.melijnbot.database.playlist

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class PlaylistWrapper(private val playlistDao: PlaylistDao) {

    suspend fun getPlaylists(userId: Long): Map<String, Map<Int, String>> {
        val result = playlistDao.getCacheEntry(userId, HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<String, Map<Int, String>>>(it)
        }

        if (result != null) return result

        val prefixes = playlistDao.getPlaylists(userId)
        playlistDao.setCacheEntry(userId, objectMapper.writeValueAsString(prefixes), NORMAL_CACHE)
        return prefixes
    }

    suspend fun set(userId: Long, playlist: String, position: Int, track: String) {
        val map = getPlaylists(userId).toMutableMap()
        val tracks = map[playlist]?.toMutableMap() ?: mutableMapOf()
        tracks[position] = track
        map[playlist] = tracks

        playlistDao.setCacheEntry("$userId", objectMapper.writeValueAsString(map), NORMAL_CACHE)
        playlistDao.set(userId, playlist, position, track)
    }

    suspend fun remove(userId: Long, playlist: String, position: Int) {
        val map = getPlaylists(userId).toMutableMap()
        val tracks = map[playlist]?.toMutableMap() ?: mutableMapOf()
        tracks.remove(position)

        if (tracks.isEmpty()) {
            map.remove(playlist)
        } else {
            map[playlist] = tracks
        }

        playlistDao.setCacheEntry("$userId", objectMapper.writeValueAsString(map), NORMAL_CACHE)
        playlistDao.removeById(userId, playlist, position)
    }

    suspend fun removeAll(userId: Long, playlist: String, positions: List<Int>) {
        if (positions.size == 1) {
            remove(userId, playlist, positions.first())
        } else {
            playlistDao.removeByIds(userId, playlist, positions)
        }
    }
}