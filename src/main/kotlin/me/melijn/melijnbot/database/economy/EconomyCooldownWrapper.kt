package me.melijn.melijnbot.database.economy

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class EconomyCooldownWrapper(private val economyCooldownDao: EconomyCooldownDao) {

    suspend fun getCooldown(userId: Long, key: String): Long {
        val cached = economyCooldownDao.getCacheEntry("$key:$userId", HIGHER_CACHE)?.toLong()
        if (cached != null) return cached

        val time = economyCooldownDao.get(userId, key)
        economyCooldownDao.setCacheEntry("$key:$userId", time, NORMAL_CACHE)
        return time
    }

    fun setCooldown(userId: Long, key: String, time: Long) {
        economyCooldownDao.set(userId, key, time)
        economyCooldownDao.setCacheEntry("$key:$userId", time, NORMAL_CACHE)
    }
}