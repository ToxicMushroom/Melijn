package me.melijn.melijnbot.database.embed

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class UserEmbedColorWrapper(val taskManager: TaskManager, private val userEmbedColorDao: UserEmbedColorDao) {

    val userEmbedColorCache = CacheBuilder.newBuilder()
        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Int> { key ->
            getColor(key)
        })

    private fun getColor(userId: Long): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        taskManager.async {
            userEmbedColorDao.get(userId) {
                future.complete(it)
            }
        }
        return future
    }

    suspend fun setColor(userId: Long, color: Int) {
        userEmbedColorCache.put(userId, CompletableFuture.completedFuture(color))
        userEmbedColorDao.set(userId, color)
    }

    suspend fun removeColor(userId: Long) {
        userEmbedColorCache.put(userId, CompletableFuture.completedFuture(0))
        userEmbedColorDao.remove(userId)
    }
}