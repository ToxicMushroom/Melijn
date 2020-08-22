package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class EmbedColorWrapper(private val embedColorDao: EmbedColorDao) {

    suspend fun getColor(guildId: Long): Int {
        val cached = embedColorDao.getCacheEntry(guildId, HIGHER_CACHE)?.toInt()
        if (cached != null) return cached

        val color = embedColorDao.get(guildId)
        embedColorDao.setCacheEntry(guildId, color, NORMAL_CACHE)
        return color
    }

    fun setColor(guildId: Long, color: Int) {
        embedColorDao.set(guildId, color)
        embedColorDao.setCacheEntry(guildId, color, NORMAL_CACHE)
    }

    fun removeColor(guildId: Long) {
        embedColorDao.remove(guildId)
        embedColorDao.setCacheEntry(guildId, 0, NORMAL_CACHE)
    }
}