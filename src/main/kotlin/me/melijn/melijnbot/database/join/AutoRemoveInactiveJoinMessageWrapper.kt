package me.melijn.melijnbot.database.join

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class AutoRemoveInactiveJoinMessageWrapper(private val autoRemoveInactiveJoinMessageDao: AutoRemoveInactiveJoinMessageDao) {

    fun set(guildId: Long, duration: Long) {
        autoRemoveInactiveJoinMessageDao.setCacheEntry(guildId, duration, NORMAL_CACHE)
        return autoRemoveInactiveJoinMessageDao.set(guildId, duration)
    }

    fun delete(guildId: Long) {
        autoRemoveInactiveJoinMessageDao.setCacheEntry(guildId, -1, NORMAL_CACHE)
        return autoRemoveInactiveJoinMessageDao.delete(guildId)
    }

    suspend fun get(guildId: Long): Long {
        val cached = autoRemoveInactiveJoinMessageDao.getLongFromCache(guildId, HIGHER_CACHE)
        if (cached != null) {
            return cached
        }

        val result = autoRemoveInactiveJoinMessageDao.get(guildId)
        autoRemoveInactiveJoinMessageDao.setCacheEntry(guildId, result, NORMAL_CACHE)
        return result
    }
}