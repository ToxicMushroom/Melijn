package me.melijn.melijnbot.database.economy

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class DailyCooldownWrapper(private val dailyCooldownDao: DailyCooldownDao) {

    suspend fun getCooldown(userId: Long): Long {
        val cached = dailyCooldownDao.getCacheEntry(userId, HIGHER_CACHE)?.toLong()
        if (cached != null) return cached

        val time = dailyCooldownDao.get(userId)
        dailyCooldownDao.setCacheEntry(userId, time, NORMAL_CACHE)
        return time
    }

    fun setCooldown(userId: Long, time: Long) {
        dailyCooldownDao.set(userId, time)
        dailyCooldownDao.setCacheEntry(userId, time, NORMAL_CACHE)
    }
}