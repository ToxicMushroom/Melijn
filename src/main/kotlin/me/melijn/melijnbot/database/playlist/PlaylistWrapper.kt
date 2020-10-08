package me.melijn.melijnbot.database.playlist

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class PlaylistWrapper(val playlistDao: PlaylistDao) {

    suspend fun getPlaylists(userId: Long): Map<String, Map<Int, String>> {
        val result = playlistDao.getCacheEntry(userId, HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<String, Map<Int, String>>>(it)
        }

        if (result != null) return result

        val prefixes = playlistDao.getPlaylists(userId)
        playlistDao.setCacheEntry(userId, objectMapper.writeValueAsString(prefixes), NORMAL_CACHE)
        return prefixes
    }
}