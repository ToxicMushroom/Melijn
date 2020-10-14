package me.melijn.melijnbot.database.cooldown

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class GlobalCooldownWrapper(private val globalCooldownDao: GlobalCooldownDao) {

    suspend fun getLastExecuted(userId: Long, commandId: String): Long {
        val cached = globalCooldownDao.getCacheEntry("$userId:$commandId", HIGHER_CACHE)?.toLong()

        if (cached != null) return cached

        val result = globalCooldownDao.getLastExecuted(userId, commandId)
        globalCooldownDao.setCacheEntry("$userId:$commandId", result, NORMAL_CACHE)
        return result
    }

    fun setLastExecuted(userId: Long, commandId: String, lastExecuted: Long) {
        globalCooldownDao.insert(userId, commandId, lastExecuted)
        globalCooldownDao.setCacheEntry("$userId:$commandId", lastExecuted, NORMAL_CACHE)
    }

}