package me.melijn.melijnbot.database.permission

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objectMapper

class ChannelUserPermissionWrapper(private val channelUserPermissionDao: ChannelUserPermissionDao) {

    suspend fun getPermMap(channelId: Long, userId: Long): Map<String, PermState> {
        val result = channelUserPermissionDao.getCacheEntry("$channelId:$userId", HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<String, PermState>>(it)
        }

        if (result != null) return result

        val permissionMap = channelUserPermissionDao.getMap(channelId, userId)
        channelUserPermissionDao.setCacheEntry(
            "$channelId:$userId",
            objectMapper.writeValueAsString(permissionMap),
            NORMAL_CACHE
        )
        return permissionMap
    }

    suspend fun setPermissions(
        guildId: Long,
        channelId: Long,
        userId: Long,
        permissions: List<String>,
        state: PermState
    ) {
        val permissionMap = getPermMap(channelId, userId).toMutableMap()
        if (state == PermState.DEFAULT) {
            permissions.forEach { permissionMap.remove(it) }
            channelUserPermissionDao.bulkDelete(channelId, userId, permissions)
        } else {
            permissions.forEach { permissionMap[it] = state }
            channelUserPermissionDao.bulkPut(guildId, channelId, userId, permissions, state)
        }
        channelUserPermissionDao.setCacheEntry(
            "$channelId:$userId",
            objectMapper.writeValueAsString(permissionMap),
            NORMAL_CACHE
        )
    }

    suspend fun setPermission(guildId: Long, channelId: Long, userId: Long, permission: String, state: PermState) {
        val permissionMap = getPermMap(channelId, userId).toMutableMap()
        if (state == PermState.DEFAULT) {
            permissionMap.remove(permission)
            channelUserPermissionDao.delete(channelId, userId, permission)
        } else {
            permissionMap[permission] = state
            channelUserPermissionDao.set(guildId, channelId, userId, permission, state)
        }
        channelUserPermissionDao.setCacheEntry(
            "$channelId:$userId",
            objectMapper.writeValueAsString(permissionMap),
            NORMAL_CACHE
        )
    }

    fun clear(channelId: Long, userId: Long) {
        channelUserPermissionDao.setCacheEntry(
            "$channelId:$userId",
            objectMapper.writeValueAsString(emptyMap<String, PermState>()),
            NORMAL_CACHE
        )
        channelUserPermissionDao.delete(channelId, userId)
    }

    suspend fun setPermissions(guildId: Long, channelId: Long, userId: Long, permissions: Map<String, PermState>) {
        for (state in PermState.values()) {
            setPermissions(guildId, channelId, userId, permissions.filter { entry ->
                entry.value == state
            }.keys.toList(), state)
        }
    }

    fun migrateChannel(oldId: Long, newId: Long) {
        channelUserPermissionDao.migrateChannel(oldId, newId)
    }
}