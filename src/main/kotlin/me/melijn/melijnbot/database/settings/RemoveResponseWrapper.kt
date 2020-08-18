package me.melijn.melijnbot.database.settings

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class RemoveResponseWrapper(
    private val removeResponsesDao: RemoveResponsesDao
) {

    suspend fun getMap(guildId: Long): Map<Long, Int> {
        val result = removeResponsesDao.getCacheEntry(guildId, HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<Long, Int>>(it)
        }

        if (result != null) return result

        val channels = removeResponsesDao.getChannels(guildId)
        removeResponsesDao.setCacheEntry(guildId, objectMapper.writeValueAsString(channels), NORMAL_CACHE)
        return channels
    }

    suspend fun set(guildId: Long, channelId: Long, seconds: Int) {
        val map = getMap(guildId).toMutableMap()
        map[channelId] = seconds

        removeResponsesDao.insert(guildId, channelId, seconds)
        removeResponsesDao.setCacheEntry("$guildId", objectMapper.writeValueAsString(map), NORMAL_CACHE)
    }

    suspend fun remove(guildId: Long, channelId: Long) {
        val map = getMap(guildId).toMutableMap()
        map.remove(channelId)

        removeResponsesDao.remove(guildId, channelId)
        removeResponsesDao.setCacheEntry("$guildId", objectMapper.writeValueAsString(map), NORMAL_CACHE)
    }
}