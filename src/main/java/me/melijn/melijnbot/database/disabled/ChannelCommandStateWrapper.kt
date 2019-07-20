package me.melijn.melijnbot.database.disabled

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.ChannelCommandState
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class ChannelCommandStateWrapper(val taskManager: TaskManager, private val channelCommandStateDao: ChannelCommandStateDao) {

    val channelCommandsStateCache = Caffeine.newBuilder()
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.getExecutorService())
            .buildAsync<Long, Map<Int, ChannelCommandState>>() { key, executor -> getCommandStateMap(key, executor) }

    fun getCommandStateMap(channelId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Map<Int, ChannelCommandState>> {
        val future = CompletableFuture<Map<Int, ChannelCommandState>>()
        executor.execute {
            channelCommandStateDao.get(channelId, Consumer {
                future.complete(it)
            })
        }
        return future
    }
}