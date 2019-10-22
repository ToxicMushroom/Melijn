package me.melijn.melijnbot.database.embed

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class UserEmbedColorWrapper(val taskManager: TaskManager, private val userEmbedColorDao: UserEmbedColorDao) {

    val userEmbedColorCache = Caffeine.newBuilder()
        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Long, Int>() { key, executor -> getColor(key, executor) }

    private fun getColor(userId: Long, executor: Executor = taskManager.executorService): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        executor.execute {
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
        userEmbedColorCache.put(userId, CompletableFuture.completedFuture(-1))
        userEmbedColorDao.remove(userId)
    }
}