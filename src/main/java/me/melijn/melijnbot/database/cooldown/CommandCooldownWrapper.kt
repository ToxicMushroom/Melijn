package me.melijn.melijnbot.database.cooldown

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class CommandCooldownWrapper(val taskManager: TaskManager, val commandCooldownDao: CommandCooldownDao) {

    val commandCooldownCache = Caffeine.newBuilder()
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.getExecutorService())
            .buildAsync<Long, Map<Int, Long>>() { key, executor -> getMap(key, executor) }

    fun getMap(guildId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Map<Int, Long>> {
        val future = CompletableFuture<Map<Int, Long>>()
        executor.execute {
            commandCooldownDao.getCooldowns(guildId, Consumer {
                future.complete(it)
            })
        }
        return future
    }
}