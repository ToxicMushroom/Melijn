package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class UserEmbedColorWrapper(private val userEmbedColorDao: UserEmbedColorDao) {

    suspend fun getColor(userId: Long): Int {
        val cached = userEmbedColorDao.getCacheEntry(userId, HIGHER_CACHE)?.toInt()
        if (cached != null) return cached

        val color = userEmbedColorDao.get(userId)
        userEmbedColorDao.setCacheEntry(userId, color, NORMAL_CACHE)
        return color
    }

    fun setColor(userId: Long, color: Int) {
        userEmbedColorDao.set(userId, color)
        userEmbedColorDao.setCacheEntry(userId, color, NORMAL_CACHE)
    }

    fun removeColor(userId: Long) {
        userEmbedColorDao.remove(userId)
        userEmbedColorDao.setCacheEntry(userId, 0, NORMAL_CACHE)
    }
}