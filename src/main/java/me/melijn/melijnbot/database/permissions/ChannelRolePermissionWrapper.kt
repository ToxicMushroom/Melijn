package me.melijn.melijnbot.database.permissions

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class ChannelRolePermissionWrapper(val taskManager: TaskManager, private val channelRolePermissionDao: ChannelRolePermissionDao) {
    val channelRolePermissionCache = Caffeine.newBuilder()
            .executor(taskManager.getExecutorService())
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .buildAsync<Pair<Long, Long>, Map<String, PermState>>() { pair, executor -> getPermissionList(pair.first, pair.second, executor) }

    fun getPermissionList(channelId: Long, roleId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Map<String, PermState>> {
        val languageFuture = CompletableFuture<Map<String, PermState>>()
        executor.execute {
            channelRolePermissionDao.getMap(channelId, roleId, Consumer { map ->
                languageFuture.complete(map)
            })
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

    fun setPermission(guildId: Long, channelId: Long, roleId: Long, permission: String, state: PermState) {
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

    fun clear(channelId: Long, roleId: Long) {
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