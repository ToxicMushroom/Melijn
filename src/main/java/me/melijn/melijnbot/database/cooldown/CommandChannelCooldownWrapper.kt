package me.melijn.melijnbot.database.cooldown

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class CommandChannelCooldownWrapper(val taskManager: TaskManager, val commandChannelCooldownDao: CommandChannelCooldownDao) {

    val commandChannelCooldownCache = Caffeine.newBuilder()
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.getExecutorService())
            .buildAsync<Long, Map<Int, Long>>() { key, executor -> }

    fun getCommancChannelCooldowns(channelId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Map<Int, Long>> {
        val map = CompletableFuture<Map<Int, Long>>()
        executor.execute {
            commandChannelCooldownDao.getCooldownMapForChannel(channelId, Consumer {
                map.complete(it)
            })
        }
        return map
    }

}