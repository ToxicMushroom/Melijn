package me.melijn.melijnbot.database.newyear

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class NewYearWrapper(private val newYearDao: NewYearDao) {

    fun add(year: Int, userId: Long) {
        newYearDao.add(year, userId)
        newYearDao.setCacheEntry("$year:$userId", true, NORMAL_CACHE)
    }

    suspend fun contains(year: Int, userId: Long): Boolean {
        val cached = newYearDao.getCacheEntry("$year:$userId", HIGHER_CACHE)?.let {
            it.toBoolean()
        }
        if (cached != null) return cached

        val result = newYearDao.contains(year, userId)
        newYearDao.setCacheEntry("$year:$userId", result, NORMAL_CACHE)
        return result
    }
}