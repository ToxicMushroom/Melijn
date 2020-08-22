package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class Music247Wrapper(private val music247Dao: Music247Dao) {

    suspend fun is247Mode(guildId: Long): Boolean {
        val cached = music247Dao.getCacheEntry(guildId, HIGHER_CACHE)?.toBoolean()
        if (cached != null) return cached

        val mode = music247Dao.contains(guildId)
        music247Dao.setCacheEntry(guildId, mode, NORMAL_CACHE)
        return mode
    }

    fun add(guildId: Long) {
        music247Dao.add(guildId)
        music247Dao.setCacheEntry(guildId, true, NORMAL_CACHE)
    }

    fun remove(guildId: Long) {
        music247Dao.remove(guildId)
        music247Dao.setCacheEntry(guildId, false, NORMAL_CACHE)
    }

}