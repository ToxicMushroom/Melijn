package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class BannedOrKickedTriggersLeaveWrapper(
    private val bannedOrKickedTriggersLeaveDao: BannedOrKickedTriggersLeaveDao
) {

    suspend fun shouldTrigger(guildId: Long): Boolean {
        val result = bannedOrKickedTriggersLeaveDao.getCacheEntry(guildId, HIGHER_CACHE)?.toBoolean()

        if (result == null) {
            val state = bannedOrKickedTriggersLeaveDao.contains(guildId)
            bannedOrKickedTriggersLeaveDao.setCacheEntry(guildId, state, NORMAL_CACHE)
            return state
        }

        return result
    }

    fun set(guildId: Long, state: Boolean) {
        if (state) {
            bannedOrKickedTriggersLeaveDao.add(guildId)
        } else {
            bannedOrKickedTriggersLeaveDao.remove(guildId)
        }

        bannedOrKickedTriggersLeaveDao.setCacheEntry(guildId, state, NORMAL_CACHE)
    }
}