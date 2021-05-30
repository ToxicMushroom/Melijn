package me.melijn.melijnbot.database.audio

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class GainProfileWrapper(private val gainProfileDao: GainProfileDao) {

    suspend fun getGainProfile(id: Long): Map<String, GainProfile> {
        val cached = gainProfileDao.getCacheEntry(id, HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<String, GainProfile>>(it)
        }
        if (cached != null) return cached

        val gps = gainProfileDao.get(id)
        gainProfileDao.setCacheEntry(id, objectMapper.writeValueAsString(gps), NORMAL_CACHE)
        return gps
    }

    suspend fun add(id: Long, name: String, bands: FloatArray) {
        val map = getGainProfile(id).toMutableMap()
        map[name] = GainProfile.fromString(bands.joinToString(","))

        gainProfileDao.insert(id, name, bands)
        gainProfileDao.setCacheEntry(id, objectMapper.writeValueAsString(map), NORMAL_CACHE)
    }

    suspend fun remove(guildId: Long, profileName: String) {
        val map = getGainProfile(guildId).toMutableMap()
        map.remove(profileName)
        gainProfileDao.delete(guildId, profileName)
        gainProfileDao.setCacheEntry(guildId, objectMapper.writeValueAsString(map), NORMAL_CACHE)
    }

    suspend fun getProfileCount(id: Long): Int {
        return gainProfileDao.getCount(id)
    }
}