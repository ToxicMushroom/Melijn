package me.melijn.melijnbot.database.permission

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objectMapper

class RolePermissionWrapper(private val rolePermissionDao: RolePermissionDao) {

    suspend fun getPermMap(roleId: Long): Map<String, PermState> {
        val result = rolePermissionDao.getCacheEntry(roleId, HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<String, PermState>>(it)
        }

        if (result != null) return result

        val permissionMap = rolePermissionDao.getMap(roleId)
        rolePermissionDao.setCacheEntry(roleId, objectMapper.writeValueAsString(permissionMap), NORMAL_CACHE)
        return permissionMap
    }

    suspend fun setPermissions(guildId: Long, roleId: Long, permissions: List<String>, state: PermState) {
        val permissionMap = getPermMap(roleId).toMutableMap()
        if (state == PermState.DEFAULT) {
            permissions.forEach { permissionMap.remove(it) }
            rolePermissionDao.bulkDelete(roleId, permissions)
        } else {
            permissions.forEach { permissionMap[it] = state }
            rolePermissionDao.bulkPut(guildId, roleId, permissions, state)
        }
        rolePermissionDao.setCacheEntry(roleId, objectMapper.writeValueAsString(permissionMap), NORMAL_CACHE)
    }

    suspend fun setPermission(guildId: Long, roleId: Long, permission: String, state: PermState) {
        val permissionMap = getPermMap(roleId).toMutableMap()
        if (state == PermState.DEFAULT) {
            permissionMap.remove(permission)
            rolePermissionDao.delete(roleId, permission)
        } else {
            permissionMap[permission] = state
            rolePermissionDao.set(guildId, roleId, permission, state)
        }
        rolePermissionDao.setCacheEntry(roleId, objectMapper.writeValueAsString(permissionMap), NORMAL_CACHE)
    }

    fun clear(roleId: Long) {
        rolePermissionDao.setCacheEntry(roleId, objectMapper.writeValueAsString(emptyMap<String, PermState>()), NORMAL_CACHE)
        rolePermissionDao.delete(roleId)
    }

    suspend fun setPermissions(guildId: Long, roleId: Long, permissions: Map<String, PermState>) {
        for (state in PermState.values()) {
            setPermissions(guildId, roleId, permissions.filter { entry ->
                entry.value == state
            }.keys.toList(), state)
        }
    }
}