package me.melijn.melijnbot.database.games

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class OsuWrapper(private val osuDao: OsuDao) {

    suspend fun getUserName(userId: Long): String {
        val result = osuDao.getCacheEntry(userId, HIGHER_CACHE)
        if (result != null) return result

        val name = osuDao.get(userId)
        osuDao.setCacheEntry(userId, name, NORMAL_CACHE)
        return name
    }

    fun setName(userId: Long, name: String) {
        osuDao.set(userId, name)
        osuDao.setCacheEntry(userId, name, NORMAL_CACHE)
    }

    fun remove(userId: Long) {
        osuDao.remove(userId)
        osuDao.setCacheEntry(userId, "", NORMAL_CACHE)
    }
}