package me.melijn.melijnbot.database.channel

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class ChannelWrapper(private val taskManager: TaskManager, private val channelDao: ChannelDao) {

    val logChannelCache = Caffeine.newBuilder()
            .executor(taskManager.executorService)
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .buildAsync<Pair<Long, ChannelType>, Long>() { key, executor -> getChannelId(key.first, key.second, executor) }

    private fun getChannelId(guildId: Long, channelType: ChannelType, executor: Executor = taskManager.executorService): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()

        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            val channelId = channelDao.get(guildId, channelType)
            future.complete(channelId)
        }

        return future
    }

    fun removeChannel(guildId: Long, channelType: ChannelType) {
        channelDao.remove(guildId, channelType)
        logChannelCache.put(Pair(guildId, channelType), CompletableFuture.completedFuture(-1))
    }

    fun setChannel(guildId: Long, channelType: ChannelType, channelId: Long) {
        channelDao.set(guildId, channelType, channelId)
        logChannelCache.put(Pair(guildId, channelType), CompletableFuture.completedFuture(channelId))
    }
}