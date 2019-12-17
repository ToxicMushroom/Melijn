package me.melijn.melijnbot.database.role

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class SelfRoleWrapper(val taskManager: TaskManager, private val selfRoleDao: SelfRoleDao) {

    val selfRoleCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Map<String, Long>> { key ->
            getMap(key)
        })

    fun getMap(guildId: Long): CompletableFuture<Map<String, Long>> {
        val future = CompletableFuture<Map<String, Long>>()
        taskManager.async {
            val map = selfRoleDao.getMap(guildId)
            future.complete(map)
        }
        return future
    }

    suspend fun set(guildId: Long, emoteji: String, roleId: Long) {
        val map = selfRoleCache.get(guildId).await().toMutableMap()
        map[emoteji] = roleId
        selfRoleCache.put(guildId, CompletableFuture.completedFuture(map))
        selfRoleDao.set(guildId, emoteji, roleId)
    }

    suspend fun remove(guildId: Long, emoteji: String) {
        val map = selfRoleCache.get(guildId).await().toMutableMap()
        map.remove(emoteji)
        selfRoleCache.put(guildId, CompletableFuture.completedFuture(map))
        selfRoleDao.remove(guildId, emoteji)
    }


}