package me.melijn.melijnbot.database.permission

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objectMapper

class ChannelRolePermissionWrapper(private val channelRolePermissionDao: ChannelRolePermissionDao) {


    suspend fun getPermMap(channelId: Long, roleId: Long): Map<String, PermState> {
        val result = channelRolePermissionDao.getCacheEntry("$channelId:$roleId", HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<String, PermState>>(it)
        }
        if (result != null) return result

        val permissionMap = channelRolePermissionDao.getMap(channelId, roleId)
        channelRolePermissionDao.setCacheEntry("$channelId:$roleId", objectMapper.writeValueAsString(permissionMap), NORMAL_CACHE)
        return permissionMap
    }

    suspend fun setPermissions(guildId: Long, channelId: Long, roleId: Long, permissions: List<String>, state: PermState) {
        val permissionMap = getPermMap(channelId, roleId).toMutableMap()
        if (state == PermState.DEFAULT) {
            permissions.forEach { permissionMap.remove(it) }
            channelRolePermissionDao.bulkDelete(channelId, roleId, permissions)
        } else {
            permissions.forEach { permissionMap[it] = state }
            channelRolePermissionDao.bulkPut(guildId, channelId, roleId, permissions, state)
        }
        channelRolePermissionDao.setCacheEntry("$channelId:$roleId", objectMapper.writeValueAsString(permissionMap), NORMAL_CACHE)
    }

    suspend fun setPermission(guildId: Long, channelId: Long, roleId: Long, permission: String, state: PermState) {
        val permissionMap = getPermMap(channelId, roleId).toMutableMap()
        if (state == PermState.DEFAULT) {
            permissionMap.remove(permission)
            channelRolePermissionDao.delete(channelId, roleId, permission)
        } else {
            permissionMap[permission] = state
            channelRolePermissionDao.set(guildId, channelId, roleId, permission, state)
        }
        channelRolePermissionDao.setCacheEntry("$channelId:$roleId", objectMapper.writeValueAsString(permissionMap), NORMAL_CACHE)
    }

    fun clear(channelId: Long, roleId: Long) {
        channelRolePermissionDao.setCacheEntry("$channelId:$roleId", objectMapper.writeValueAsString(emptyMap<String, PermState>()), NORMAL_CACHE)
        channelRolePermissionDao.delete(channelId, roleId)
    }

    suspend fun setPermissions(guildId: Long, channelId: Long, roleId: Long, permissions: Map<String, PermState>) {
        for (state in PermState.values()) {
            setPermissions(guildId, channelId, roleId, permissions.filter { entry ->
                entry.value == state
            }.keys.toList(), state)
        }
    }

    fun migrateChannel(oldId: Long, newId: Long) {
        channelRolePermissionDao.migrateChannel(oldId, newId)
    }
}