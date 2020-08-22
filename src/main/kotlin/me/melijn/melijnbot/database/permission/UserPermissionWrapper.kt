package me.melijn.melijnbot.database.permission

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objectMapper

class UserPermissionWrapper(private val userPermissionDao: UserPermissionDao) {

    suspend fun getPermMap(guildId: Long, userId: Long): Map<String, PermState> {
        val result = userPermissionDao.getCacheEntry("$guildId:$userId", HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<String, PermState>>(it)
        }

        if (result != null) return result

        val permissionMap = userPermissionDao.getMap(guildId, userId)
        userPermissionDao.setCacheEntry("$guildId:$userId", objectMapper.writeValueAsString(permissionMap), NORMAL_CACHE)
        return permissionMap
    }

    suspend fun setPermissions(guildId: Long, userId: Long, permissions: List<String>, state: PermState) {
        val permissionMap = getPermMap(guildId, userId).toMutableMap()
        if (state == PermState.DEFAULT) {
            permissions.forEach { permissionMap.remove(it) }
            userPermissionDao.bulkDelete(guildId, userId, permissions)
        } else {
            permissions.forEach { permissionMap[it] = state }
            userPermissionDao.bulkPut(guildId, userId, permissions, state)
        }
        userPermissionDao.setCacheEntry("$guildId:$userId", objectMapper.writeValueAsString(permissionMap), NORMAL_CACHE)
    }

    suspend fun setPermission(guildId: Long, userId: Long, permission: String, state: PermState) {
        val permissionMap = getPermMap(guildId, userId).toMutableMap()
        if (state == PermState.DEFAULT) {
            permissionMap.remove(permission)
            userPermissionDao.delete(guildId, userId, permission)
        } else {
            permissionMap[permission] = state
            userPermissionDao.set(guildId, userId, permission, state)
        }
        userPermissionDao.setCacheEntry("$guildId:$userId", objectMapper.writeValueAsString(permissionMap), NORMAL_CACHE)
    }

    fun clear(guildId: Long, userId: Long) {
        userPermissionDao.setCacheEntry("$guildId:$userId", objectMapper.writeValueAsString(emptyMap<String, PermState>()), NORMAL_CACHE)
        userPermissionDao.delete(guildId, userId)
    }

    suspend fun setPermissions(guildId: Long, userId: Long, permissions: Map<String, PermState>) {
        for (state in PermState.values()) {
            setPermissions(guildId, userId, permissions.filter { entry ->
                entry.value == state
            }.keys.toList(), state)
        }
    }
}