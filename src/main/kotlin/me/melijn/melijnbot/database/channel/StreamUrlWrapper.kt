package me.melijn.melijnbot.database.channel

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class StreamUrlWrapper(private val streamUrlDao: StreamUrlDao) {

    suspend fun getUrl(guildId: Long): String {
        val cached = streamUrlDao.getCacheEntry(guildId, HIGHER_CACHE)
        if (cached != null) return cached

        val result = streamUrlDao.get(guildId)
        streamUrlDao.setCacheEntry(guildId, result, NORMAL_CACHE)
        return result
    }

    suspend fun setUrl(guildId: Long, url: String) {
        streamUrlDao.set(guildId, url)
        streamUrlDao.setCacheEntry(guildId, url, NORMAL_CACHE)
    }

    suspend fun removeUrl(guildId: Long) {
        streamUrlDao.remove(guildId)
        streamUrlDao.setCacheEntry(guildId, "", NORMAL_CACHE)
    }

}