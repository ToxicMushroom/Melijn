package me.melijn.melijnbot.database.channel

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class StreamUrlWrapper(val taskManager: TaskManager, private val streamUrlDao: StreamUrlDao) {

    val streamUrlCache = Caffeine.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Long, String>() { key, executor -> getCode(key, executor) }

    private fun getCode(guildId: Long, executor: Executor): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        executor.launch {
            val url = streamUrlDao.get(guildId)
            future.complete(url)
        }
        return future
    }

    suspend fun setUrl(guildId: Long, url: String) {
        streamUrlCache.put(guildId, CompletableFuture.completedFuture(url))
        streamUrlDao.set(guildId, url)
    }

    suspend fun removeUrl(guildId: Long) {
        streamUrlCache.put(guildId, CompletableFuture.completedFuture(""))
        streamUrlDao.remove(guildId)
    }

}