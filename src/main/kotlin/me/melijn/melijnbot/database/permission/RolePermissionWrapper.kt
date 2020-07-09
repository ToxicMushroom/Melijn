package me.melijn.melijnbot.database.permission

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class RolePermissionWrapper(val taskManager: TaskManager, private val rolePermissionDao: RolePermissionDao) {

    val rolePermissionCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Map<String, PermState>> { key ->
            getPermissionList(key)
        })

    private fun getPermissionList(roleId: Long): CompletableFuture<Map<String, PermState>> {
        val languageFuture = CompletableFuture<Map<String, PermState>>()
        taskManager.async {
            val map = rolePermissionDao.getMap(roleId)
            languageFuture.complete(map)
        }
        return languageFuture
    }

    suspend fun setPermissions(guildId: Long, roleId: Long, permissions: List<String>, state: PermState) {
        val permissionMap = rolePermissionCache.get(roleId).await().toMutableMap()
        if (state == PermState.DEFAULT) {
            permissions.forEach { permissionMap.remove(it) }
            rolePermissionDao.bulkDelete(roleId, permissions)
        } else {
            permissions.forEach { permissionMap[it] = state }
            rolePermissionDao.bulkPut(guildId, roleId, permissions, state)
        }
        rolePermissionCache.put(roleId, CompletableFuture.completedFuture(permissionMap.toMap()))
    }

    suspend fun setPermission(guildId: Long, roleId: Long, permission: String, state: PermState) {
        val permissionMap = rolePermissionCache.get(roleId).await().toMutableMap()
        if (state == PermState.DEFAULT) {
            permissionMap.remove(permission)
            rolePermissionDao.delete(roleId, permission)
        } else {
            permissionMap[permission] = state
            rolePermissionDao.set(guildId, roleId, permission, state)
        }
        rolePermissionCache.put(roleId, CompletableFuture.completedFuture(permissionMap.toMap()))
    }

    suspend fun clear(roleId: Long) {
        rolePermissionCache.put(roleId, CompletableFuture.completedFuture(emptyMap()))
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