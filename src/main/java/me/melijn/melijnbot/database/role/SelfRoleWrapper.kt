package me.melijn.melijnbot.database.role

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class SelfRoleWrapper(private val taskManager: TaskManager, private val selfRoleDao: SelfRoleDao) {

    val selfRoleCache = Caffeine.newBuilder()
        .executor(taskManager.executorService)
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .buildAsync<Long, Map<Long, Long>> { key, executor ->
            getMap(key, executor)
        }

    fun getMap(guildId: Long, executor: Executor): CompletableFuture<Map<Long, Long>> {
        val future = CompletableFuture<Map<Long, Long>>()
        executor.launch {
            val map = selfRoleDao.getMap(guildId)
            future.complete(map)
        }
        return future
    }

    suspend fun set(guildId: Long, emoteId: Long, roleId:  Long) {
        val map = selfRoleCache.get(guildId).await().toMutableMap()
        map[emoteId] = roleId
        selfRoleCache.put(guildId, CompletableFuture.completedFuture(map))
        selfRoleDao.set(guildId, emoteId, roleId)
    }

    suspend fun remove(guildId: Long, emoteId: Long) {
        val map = selfRoleCache.get(guildId).await().toMutableMap()
        map.remove(emoteId)
        selfRoleCache.put(guildId, CompletableFuture.completedFuture(map))
        selfRoleDao.remove(guildId, emoteId)
    }


}