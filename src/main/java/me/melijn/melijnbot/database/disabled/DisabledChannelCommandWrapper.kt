package me.melijn.melijnbot.database.disabled

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class DisabledChannelCommandWrapper(val taskManager: TaskManager, val disabledChannelCommandDao: DisabledChannelCommandDao) {

    val disabledChanneldCommandsCache = Caffeine.newBuilder()
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.getExecutorService())
            .buildAsync<Long, Set<Int>>() { key, executor -> getDisabledCommandSet(key, executor) }

    fun getDisabledCommandSet(channelId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Set<Int>> {
        val future = CompletableFuture<Set<Int>>()
        executor.execute {
            disabledChannelCommandDao.get(channelId, Consumer {
                future.complete(it)
            })
        }
        return future
    }
}