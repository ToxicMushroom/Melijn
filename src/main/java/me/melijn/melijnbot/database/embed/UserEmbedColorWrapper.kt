package me.melijn.melijnbot.database.embed

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class UserEmbedColorWrapper(val taskManager: TaskManager, val userEmbedColorDao: UserEmbedColorDao) {

    val userEmbedColorCache = Caffeine.newBuilder()
            .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.getExecutorService())
            .buildAsync<Long, Int>() { key, executor -> getColor(key, executor) }

    fun getColor(userId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        executor.execute {
            userEmbedColorDao.get(userId, Consumer {
                future.complete(it)
            })
        }
        return future
    }

    fun setColor(userId: Long, color: Int) {
        userEmbedColorCache.put(userId, CompletableFuture.completedFuture(color))
        userEmbedColorDao.set(userId, color)
    }
}