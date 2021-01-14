package me.melijn.melijnbot.database.rep

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class RepWrapper(private val repDao: RepDao) {

    suspend fun getRep(userId: Long): Int {
        val cached = repDao.getCacheEntry(userId, HIGHER_CACHE)?.toInt()
        if (cached != null) return cached

        val rep = repDao.get(userId)
        repDao.setCacheEntry(userId, rep, NORMAL_CACHE)
        return rep
    }

    fun setRep(userId: Long, rep: Int) {
        repDao.set(userId, rep)
        repDao.setCacheEntry(userId, rep, NORMAL_CACHE)
    }

    suspend fun increment(userId: Long) {
        val rep = getRep(userId)
        setRep(userId, rep + 1)
    }
}