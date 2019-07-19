package me.melijn.melijnbot.database.disabled

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class DisabledCommandWrapper(val taskManager: TaskManager, val disabledCommandDao: DisabledCommandDao) {

    val disabledCommandsCache = Caffeine.newBuilder()
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.getExecutorService())
            .buildAsync<Long, Set<Int>>() { key, executor -> getDisabledCommandSet(key, executor) }

    fun getDisabledCommandSet(guildId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Set<Int>> {
        val future = CompletableFuture<Set<Int>>()
        executor.execute {
            disabledCommandDao.get(guildId, Consumer {
                future.complete(it)
            })
        }
        return future
    }
}