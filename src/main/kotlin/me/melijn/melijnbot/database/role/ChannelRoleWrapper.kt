package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.ChannelRoleState
import me.melijn.melijnbot.internals.utils.addIfNotPresent

class ChannelRoleWrapper(private val channelRoleDao: ChannelRoleDao) {

    suspend fun set(guildId: Long, channelId: Long, roleId: Long, state: ChannelRoleState) {
        val roleIds = getRoleIds(guildId, channelId).toMutableMap()
        val stateList = roleIds[state]?.toMutableList() ?: mutableListOf()
        stateList.addIfNotPresent(roleId)
        roleIds[state] = stateList

        channelRoleDao.setCacheEntry("$guildId:$channelId", roleIds, NORMAL_CACHE)
        channelRoleDao.set(guildId, channelId, roleId, state)
    }

    suspend fun remove(guildId: Long, channelId: Long, roleId: Long) {
        val roleIds = getRoleIds(guildId, channelId).toMutableMap()
        for (state in roleIds.keys) {
            val stateList = roleIds[state]?.toMutableList() ?: mutableListOf()
            stateList.remove(roleId)

            if (stateList.isEmpty()) roleIds.remove(state)
            else roleIds[state] = stateList
        }

        channelRoleDao.setCacheEntry("$guildId:$channelId", roleIds, NORMAL_CACHE)
        channelRoleDao.remove(guildId, channelId, roleId)
    }

    suspend fun getRoleIds(guildId: Long, channelId: Long): Map<ChannelRoleState, List<Long>> {
        val cached = channelRoleDao.getValueFromCache<Map<ChannelRoleState, List<Long>>>(
            "$guildId:$channelId", HIGHER_CACHE
        )

        if (cached != null) return cached
        val result = channelRoleDao.getRoleIds(guildId, channelId)
        channelRoleDao.setCacheEntry("$guildId:$channelId", result, NORMAL_CACHE)
        return result
    }

    suspend fun getChannelRoles(guildId: Long): Map<Long, Map<ChannelRoleState, List<Long>>> {
        return channelRoleDao.getChannelRoles(guildId)
    }

    fun clear(guildId: Long, channelId: Long) {
        channelRoleDao.removeCacheEntry("$guildId:$channelId")
        channelRoleDao.clear(guildId, channelId)
    }
}

