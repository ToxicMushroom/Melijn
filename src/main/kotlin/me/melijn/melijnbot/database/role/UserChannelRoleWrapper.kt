package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.database.getValueFromCache
import me.melijn.melijnbot.enums.ChannelRoleState

class UserChannelRoleWrapper(private val userChannelRoleDao: UserChannelRoleDao) {

    fun setBulk(userId: Long, map: Map<ChannelRoleState, List<Long>>) {
        userChannelRoleDao.setBulk(userId, map)
        userChannelRoleDao.setCacheEntry(userId, map, NORMAL_CACHE)
    }

    suspend fun get(userId: Long): Map<ChannelRoleState, List<Long>> {
        val cached: Map<ChannelRoleState, List<Long>>? = userChannelRoleDao.getValueFromCache(userId, HIGHER_CACHE)
        if (cached != null) {
            return cached
        }

        val value = userChannelRoleDao.get(userId)
        userChannelRoleDao.setCacheEntry(userId, value, NORMAL_CACHE)
        return value
    }

    fun clear(userId: Long) {
        userChannelRoleDao.clear(userId)
        userChannelRoleDao.setCacheEntry(userId, emptyMap<ChannelRoleState, List<Long>>(), NORMAL_CACHE)
    }
}