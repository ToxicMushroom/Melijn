package me.melijn.melijnbot.database.logchannels

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class LogChannelWrapper(private val taskManager: TaskManager, private val logChannelDao: LogChannelDao) {

    val logChannelCache = Caffeine.newBuilder()
            .executor(taskManager.getExecutorService())
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .buildAsync<Pair<Long, LogChannelType>, Long>() { key, executor -> getChannelId(key.first, key.second, executor) }

    private fun getChannelId(guildId: Long, logChannelType: LogChannelType, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()
        executor.execute {
            logChannelDao.get(guildId, logChannelType) {
                future.complete(it)
            }
        }
        return future
    }
}