package me.melijn.melijnbot.database.role

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ForceRoleWrapper(private val taskManager: TaskManager, private val forceRoleDao: ForceRoleDao) {

    val forceRoleCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Map<Long, List<Long>>> { key ->
            getForceRoles(key)
        })

    private fun getForceRoles(guildId: Long): CompletableFuture<Map<Long, List<Long>>> {
        val future = CompletableFuture<Map<Long, List<Long>>>()
        taskManager.async {
            val roleId = forceRoleDao.getMap(guildId)
            future.complete(roleId)
        }
        return future
    }

    suspend fun add(guildId: Long, userId: Long, roleId: Long) {
        val map = forceRoleCache.get(guildId).await().toMutableMap()
        val list = map.getOrDefault(userId, emptyList()).toMutableList()
        if (!list.contains(roleId)) {
            list.add(roleId)
            forceRoleDao.add(guildId, userId, roleId)
        }
        map[userId] = list
        forceRoleCache.put(guildId, CompletableFuture.completedFuture(map))
    }

    suspend fun remove(guildId: Long, userId: Long, roleId: Long) {
        val map = forceRoleCache.get(guildId).await().toMutableMap()
        val list = map.getOrDefault(userId, emptyList()).toMutableList()
        if (list.contains(roleId)) {
            list.remove(roleId)
            forceRoleDao.remove(guildId, userId, roleId)
        }
        map[userId] = list
        forceRoleCache.put(guildId, CompletableFuture.completedFuture(map))
    }
}