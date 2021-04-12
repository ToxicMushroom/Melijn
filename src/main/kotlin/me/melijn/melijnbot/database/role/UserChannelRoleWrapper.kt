package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.enums.ChannelRoleState

class UserChannelRoleWrapper(private val userChannelRoleDao: UserChannelRoleDao) {

    fun setBulk(userId: Long, map: Map<ChannelRoleState, List<Long>>) {
        userChannelRoleDao.setBulk(userId, map)
    }

    suspend fun get(userId: Long): Map<ChannelRoleState, List<Long>> {
        return userChannelRoleDao.get(userId)
    }

    fun clear(userId: Long) {
        userChannelRoleDao.clear(userId)
    }
}