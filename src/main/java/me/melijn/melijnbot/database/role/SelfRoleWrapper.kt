package me.melijn.melijnbot.database.role

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class SelfRoleWrapper(taskManager: TaskManager, private val selfRoleDao: SelfRoleDao) {

    val selfRoleCache = Caffeine.newBuilder()
        .executor(taskManager.executorService)
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .buildAsync<Long, Map<String, Long>> { key, executor ->
            getMap(key, executor)
        }

    fun getMap(guildId: Long, executor: Executor): CompletableFuture<Map<String, Long>> {
        val future = CompletableFuture<Map<String, Long>>()
        executor.launch {
            val map = selfRoleDao.getMap(guildId)
            future.complete(map)
        }
        return future
    }

    suspend fun set(guildId: Long, emoteji: String, roleId:  Long) {
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