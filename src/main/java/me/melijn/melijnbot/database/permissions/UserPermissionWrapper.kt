package me.melijn.melijnbot.database.permissions

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class UserPermissionWrapper(val taskManager: TaskManager, private val userPermissionDao: UserPermissionDao) {
    val guildUserPermissionCache = Caffeine.newBuilder()
            .executor(taskManager.getExecutorService())
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .buildAsync<Pair<Long, Long>, Map<String, PermState>>() { key, executor -> getPermissionList(key, executor) }

    fun getPermissionList(guildAndUser: Pair<Long, Long>, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Map<String, PermState>> {
        val languageFuture = CompletableFuture<Map<String, PermState>>()
        executor.execute {
            userPermissionDao.getMap(guildAndUser.first, guildAndUser.second) { map ->
                languageFuture.complete(map)
            }
        }
        return languageFuture
    }

    fun setPermissions(guildId: Long, userId: Long, permissions: List<String>, state: PermState) {
        val pair = Pair(guildId, userId)
        val permissionMap = guildUserPermissionCache.get(pair).get().toMutableMap()
        if (state == PermState.DEFAULT) {
            permissions.forEach { permissionMap.remove(it) }
            userPermissionDao.bulkDelete(guildId, userId, permissions)
        } else {
            permissions.forEach { permissionMap[it] = state }
            userPermissionDao.bulkPut(guildId, userId, permissions, state)
        }
        guildUserPermissionCache.put(pair, CompletableFuture.completedFuture(permissionMap.toMap()))
    }

    fun setPermission(guildId: Long, userId: Long, permission: String, state: PermState) {
        val pair = Pair(guildId, userId)
        val permissionMap = guildUserPermissionCache.get(pair).get().toMutableMap()
        if (state == PermState.DEFAULT) {
            permissionMap.remove(permission)
            userPermissionDao.delete(guildId, userId, permission)
        } else {
            permissionMap[permission] = state
            userPermissionDao.set(guildId, userId, permission, state)
        }
        guildUserPermissionCache.put(pair, CompletableFuture.completedFuture(permissionMap.toMap()))
    }

    fun clear(guildId: Long, userId: Long) {
        guildUserPermissionCache.put(Pair(guildId, userId), CompletableFuture.completedFuture(emptyMap()))
        userPermissionDao.delete(guildId, userId)
    }

    fun setPermissions(guildId: Long, userId: Long, permissions: Map<String, PermState>) {
        for (state in PermState.values()) {
            setPermissions(guildId, userId, permissions.filter { entry ->
                entry.value == state
            }.keys.toList(), state)
        }
    }
}