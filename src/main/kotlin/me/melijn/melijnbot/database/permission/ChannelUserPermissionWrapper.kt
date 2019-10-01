package me.melijn.melijnbot.database.permission

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class ChannelUserPermissionWrapper(val taskManager: TaskManager, private val channelUserPermissionDao: ChannelUserPermissionDao) {

    val channelUserPermissionCache = Caffeine.newBuilder()
        .executor(taskManager.executorService)
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .buildAsync<Pair<Long, Long>, Map<String, PermState>>() { pair, executor -> getPermissionList(pair.first, pair.second, executor) }

    private fun getPermissionList(channelId: Long, userId: Long, executor: Executor = taskManager.executorService): CompletableFuture<Map<String, PermState>> {
        val languageFuture = CompletableFuture<Map<String, PermState>>()
        executor.launch {
            val map = channelUserPermissionDao.getMap(channelId, userId)
                languageFuture.complete(map)

        }
        return languageFuture
    }

    fun setPermissions(guildId: Long, channelId: Long, userId: Long, permissions: List<String>, state: PermState) {
        val permissionMap = channelUserPermissionCache.get(Pair(channelId, userId)).get().toMutableMap()
        if (state == PermState.DEFAULT) {
            permissions.forEach { permissionMap.remove(it) }
            channelUserPermissionDao.bulkDelete(channelId, userId, permissions)
        } else {
            permissions.forEach { permissionMap[it] = state }
            channelUserPermissionDao.bulkPut(guildId, channelId, userId, permissions, state)
        }
        channelUserPermissionCache.put(Pair(channelId, userId), CompletableFuture.completedFuture(permissionMap.toMap()))
    }

    suspend fun setPermission(guildId: Long, channelId: Long, userId: Long, permission: String, state: PermState) {
        val permissionMap = channelUserPermissionCache.get(Pair(channelId, userId)).await().toMutableMap()
        if (state == PermState.DEFAULT) {
            permissionMap.remove(permission)
            channelUserPermissionDao.delete(channelId, userId, permission)
        } else {
            permissionMap[permission] = state
            channelUserPermissionDao.set(guildId, channelId, userId, permission, state)
        }
        channelUserPermissionCache.put(Pair(channelId, userId), CompletableFuture.completedFuture(permissionMap.toMap()))
    }

    suspend fun clear(channelId: Long, roleId: Long) {
        channelUserPermissionCache.put(Pair(channelId, roleId), CompletableFuture.completedFuture(emptyMap()))
        channelUserPermissionDao.delete(channelId, roleId)
    }

    fun setPermissions(guildId: Long, channelId: Long, userId: Long, permissions: Map<String, PermState>) {
        for (state in PermState.values()) {
            setPermissions(guildId, channelId, userId, permissions.filter { entry ->
                entry.value == state
            }.keys.toList(), state)
        }
    }
}