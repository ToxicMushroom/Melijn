package me.melijn.melijnbot.database.starboard

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class StarboardSettingsWrapper(private val settingsDao: StarboardSettingsDao) {

    suspend fun getStarboardSettings(guildId: Long): StarboardSettings {
        val result = settingsDao.getCacheEntry(guildId, HIGHER_CACHE)?.let {
            objectMapper.readValue(it, StarboardSettings::class.java)
        }
        if (result != null) return result

        val starboardSettings = settingsDao.get(guildId)
        settingsDao.setCacheEntry(guildId, objectMapper.writeValueAsString(starboardSettings), NORMAL_CACHE)
        return starboardSettings
    }

    fun setStarboardSettings(guildId: Long, starboardSettings: StarboardSettings) {
        settingsDao.set(guildId, starboardSettings)
        settingsDao.setCacheEntry(guildId, objectMapper.writeValueAsString(starboardSettings), NORMAL_CACHE)
    }

}