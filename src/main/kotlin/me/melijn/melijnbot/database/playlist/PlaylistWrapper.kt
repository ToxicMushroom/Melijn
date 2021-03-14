package me.melijn.melijnbot.database.playlist

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.commands.music.EncodedTrack
import me.melijn.melijnbot.commands.music.Index
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class PlaylistWrapper(private val playlistDao: PlaylistDao) {

    suspend fun getPlaylists(userId: Long): Map<String, Map<Index, EncodedTrack>> {
        val result = playlistDao.getCacheEntry(userId, HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<String, Map<Index, EncodedTrack>>>(it)
        }

        if (result != null) return result

        val prefixes = playlistDao.getPlaylists(userId)
        playlistDao.setCacheEntry(userId, objectMapper.writeValueAsString(prefixes), NORMAL_CACHE)
        return prefixes
    }

    suspend fun set(userId: Long, playlist: String, position: Index, track: EncodedTrack) {
        val map = getPlaylists(userId).toMutableMap()
        val tracks = map[playlist]?.toMutableMap() ?: mutableMapOf()
        tracks[position] = track
        map[playlist] = tracks

        playlistDao.setCacheEntry("$userId", objectMapper.writeValueAsString(map), NORMAL_CACHE)
        playlistDao.set(userId, playlist, position, track)
    }

    suspend fun remove(userId: Long, playlist: String, position: Index) {
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

    suspend fun removeAll(userId: Long, playlist: String, positions: List<Index>) {
        if (positions.size == 1) {
            remove(userId, playlist, positions.first())
        } else {
            val map = getPlaylists(userId).toMutableMap()
            val tracks = map[playlist]?.toMutableMap() ?: mutableMapOf()
            for (i in positions) {
                tracks.remove(i)
            }

            if (tracks.isEmpty()) {
                map.remove(playlist)
            } else {
                map[playlist] = tracks
            }

            playlistDao.setCacheEntry("$userId", objectMapper.writeValueAsString(map), NORMAL_CACHE)
            playlistDao.removeByIds(userId, playlist, positions)
        }
    }

    suspend fun clear(userId: Long, playlist: String) {
        val map = getPlaylists(userId).toMutableMap()
        map.remove(playlist)
        playlistDao.setCacheEntry("$userId", objectMapper.writeValueAsString(map), NORMAL_CACHE)
        playlistDao.clear(userId, playlist)
    }

    fun rename(previousName: String, newName: String) {
        playlistDao.rename(previousName, newName)
    }
}