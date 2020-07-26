package me.melijn.melijnbot.database.permission

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ChannelRolePermissionWrapper(private val channelRolePermissionDao: ChannelRolePermissionDao) {

    val channelRolePermissionCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Pair<Long, Long>, Map<String, PermState>> { pair ->
            getPermissionList(pair.first, pair.second)
        })

    private fun getPermissionList(channelId: Long, roleId: Long): CompletableFuture<Map<String, PermState>> {
        val languageFuture = CompletableFuture<Map<String, PermState>>()
        TaskManager.async {
            val map = channelRolePermissionDao.getMap(channelId, roleId)
            languageFuture.complete(map)
        }
        return languageFuture
    }

    fun setPermissions(guildId: Long, channelId: Long, roleId: Long, permissions: List<String>, state: PermState) {
        val permissionMap = channelRolePermissionCache.get(Pair(channelId, roleId)).get().toMutableMap()
        if (state == PermState.DEFAULT) {
            permissions.forEach { permissionMap.remove(it) }
            channelRolePermissionDao.bulkDelete(channelId, roleId, permissions)
        } else {
            permissions.forEach { permissionMap[it] = state }
            channelRolePermissionDao.bulkPut(guildId, channelId, roleId, permissions, state)
        }
        channelRolePermissionCache.put(Pair(channelId, roleId), CompletableFuture.completedFuture(permissionMap.toMap()))
    }

    suspend fun setPermission(guildId: Long, channelId: Long, roleId: Long, permission: String, state: PermState) {
        val permissionMap = channelRolePermissionCache.get(Pair(channelId, roleId)).get().toMutableMap()
        if (state == PermState.DEFAULT) {
            permissionMap.remove(permission)
            channelRolePermissionDao.delete(channelId, roleId, permission)
        } else {
            permissionMap[permission] = state
            channelRolePermissionDao.set(guildId, channelId, roleId, permission, state)
        }
        channelRolePermissionCache.put(Pair(channelId, roleId), CompletableFuture.completedFuture(permissionMap.toMap()))
    }

    suspend fun clear(channelId: Long, roleId: Long) {
        channelRolePermissionCache.put(Pair(channelId, roleId), CompletableFuture.completedFuture(emptyMap()))
        channelRolePermissionDao.delete(channelId, roleId)
    }

    fun setPermissions(guildId: Long, channelId: Long, roleId: Long, permissions: Map<String, PermState>) {
        for (state in PermState.values()) {
            setPermissions(guildId, channelId, roleId, permissions.filter { entry ->
                entry.value == state
            }.keys.toList(), state)
        }
    }
}