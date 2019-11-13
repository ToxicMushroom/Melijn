package me.melijn.melijnbot.database.channel

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class MusicChannelWrapper(val taskManager: TaskManager, val musicChannelDao: MusicChannelDao) {

    val musicChannelCache = Caffeine.newBuilder()
        .executor(taskManager.executorService)
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .buildAsync<Long, Long>() { guildId, executor -> getChannelId(guildId, executor) }

    private fun getChannelId(guildId: Long, executor: Executor = taskManager.executorService): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()

        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            val channelId = musicChannelDao.get(guildId)
            future.complete(channelId)
        }

        return future
    }

    suspend fun removeChannel(guildId: Long) {
        musicChannelDao.remove(guildId)
        musicChannelCache.put(guildId, CompletableFuture.completedFuture(-1))
    }

    suspend fun setChannel(guildId: Long, channelId: Long) {
        musicChannelDao.set(guildId, channelId)
        musicChannelCache.put(guildId, CompletableFuture.completedFuture(channelId))
    }
}