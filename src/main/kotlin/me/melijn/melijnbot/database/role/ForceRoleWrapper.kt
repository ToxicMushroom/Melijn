package me.melijn.melijnbot.database.role

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.internals.utils.addIfNotPresent
import me.melijn.melijnbot.objectMapper

class ForceRoleWrapper(private val forceRoleDao: ForceRoleDao) {

    suspend fun getForceRoles(guildId: Long): Map<Long, List<Long>> {
        val result = forceRoleDao.getCacheEntry(guildId, HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<Long, List<Long>>>(it)
        }

        if (result != null) return result

        val forceRoles = forceRoleDao.getMap(guildId)
        forceRoleDao.setCacheEntry(guildId, objectMapper.writeValueAsString(forceRoles), NORMAL_CACHE)
        return forceRoles
    }

    suspend fun add(guildId: Long, userId: Long, roleId: Long) {
        val map = getForceRoles(guildId).toMutableMap()
        val list = map[userId]?.toMutableList() ?: mutableListOf()
        if (list.addIfNotPresent(roleId)) {
            forceRoleDao.add(guildId, userId, roleId)
        }
        map[userId] = list
        forceRoleDao.setCacheEntry(guildId, objectMapper.writeValueAsString(map), NORMAL_CACHE)
    }

    suspend fun remove(guildId: Long, userId: Long, roleId: Long) {
        val map = getForceRoles(guildId).toMutableMap()
        val list = map[userId]?.toMutableList() ?: mutableListOf()
        if (list.remove(roleId)) {
            forceRoleDao.remove(guildId, userId, roleId)
        }
        if (list.isEmpty()) {
            map.remove(userId)
        } else {
            map[userId] = list
        }
        forceRoleDao.setCacheEntry(guildId, objectMapper.writeValueAsString(map), NORMAL_CACHE)
    }
}