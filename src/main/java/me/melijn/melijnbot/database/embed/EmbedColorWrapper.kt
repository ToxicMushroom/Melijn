package me.melijn.melijnbot.database.embed

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class EmbedColorWrapper(val taskManager: TaskManager, private val embedColorDao: EmbedColorDao) {

    val embedColorCache = Caffeine.newBuilder()
            .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.executorService)
            .buildAsync<Long, Int>() { key, executor -> getColor(key, executor) }

    private fun getColor(guildId: Long, executor: Executor = taskManager.executorService): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        executor.execute {
            embedColorDao.get(guildId) {
                future.complete(it)
            }
        }
        return future
    }

    fun setColor(guildId: Long, color: Int) {
        embedColorCache.put(guildId, CompletableFuture.completedFuture(color))
        embedColorDao.set(guildId, color)
    }
}