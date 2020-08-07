package me.melijn.melijnbot.database.settings

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class RemoveInvokeWrapper(
    private val removeInvokeDao: RemoveInvokeDao
) {

    suspend fun getMap(guildId: Long): Map<Long, Int> {
        val result = removeInvokeDao.getCacheEntry(guildId, HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<Long, Int>>(it)
        }

        if (result != null) return result

        val channels = removeInvokeDao.getChannels(guildId)
        removeInvokeDao.setCacheEntry(guildId, channels, NORMAL_CACHE)
        return channels
    }

    suspend fun set(guildId: Long, channelId: Long, seconds: Int) {
        val map = getMap(guildId).toMutableMap()
        map[channelId] = seconds

        removeInvokeDao.insert(guildId, channelId, seconds)
        removeInvokeDao.setCacheEntry("$guildId", map, NORMAL_CACHE)
    }

    suspend fun remove(guildId: Long, channelId: Long) {
        val map = getMap(guildId).toMutableMap()
        map.remove(channelId)

        removeInvokeDao.remove(guildId, channelId)
        removeInvokeDao.setCacheEntry("$guildId", map, NORMAL_CACHE)
    }
}