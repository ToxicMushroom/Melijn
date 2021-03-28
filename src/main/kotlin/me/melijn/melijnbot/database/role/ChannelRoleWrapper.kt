package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.internals.utils.addIfNotPresent

class ChannelRoleWrapper(private val channelRoleDao: ChannelRoleDao) {

    suspend fun add(guildId: Long, channelId: Long, roleId: Long) {
        val roleIds = getRoleIds(guildId, channelId).toMutableList()
        roleIds.addIfNotPresent(roleId)

        channelRoleDao.setCacheEntry("$guildId:$channelId", roleIds, NORMAL_CACHE)
        channelRoleDao.add(guildId, channelId, roleId)
    }

    suspend fun remove(guildId: Long, channelId: Long, roleId: Long) {
        val roleIds = getRoleIds(guildId, channelId).toMutableList()
        roleIds.remove(roleId)

        channelRoleDao.setCacheEntry("$guildId:$channelId", roleIds, NORMAL_CACHE)
        channelRoleDao.remove(guildId, channelId, roleId)
    }

    suspend fun getRoleIds(guildId: Long, channelId: Long): List<Long> {
        val cached = channelRoleDao.getValueFromCache<List<Long>>(
            "$guildId:$channelId", HIGHER_CACHE
        )

        if (cached != null) return cached
        val result = channelRoleDao.getRoleIds(guildId, channelId)
        channelRoleDao.setCacheEntry("$guildId:$channelId", result, NORMAL_CACHE)
        return result
    }

    suspend fun getChannelRoles(guildId: Long): Map<Long, List<Long>> {
        return channelRoleDao.getChannelRoles(guildId)
    }

    fun clear(guildId: Long, channelId: Long) {
        channelRoleDao.removeCacheEntry("$guildId:$channelId")
        channelRoleDao.clear(guildId, channelId)
    }

}