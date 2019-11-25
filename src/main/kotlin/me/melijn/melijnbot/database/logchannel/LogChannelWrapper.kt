package me.melijn.melijnbot.database.logchannel

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class LogChannelWrapper(private val taskManager: TaskManager, private val logChannelDao: LogChannelDao) {

    val logChannelCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Pair<Long, LogChannelType>, Long> { key ->
            getChannelId(key.first, key.second)
        })

    private fun getChannelId(guildId: Long, logChannelType: LogChannelType): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()
        taskManager.async {
            val logChannel = logChannelDao.get(guildId, logChannelType)
            future.complete(logChannel)
        }
        return future
    }

    suspend fun removeChannel(guildId: Long, logChannelType: LogChannelType) {
        logChannelDao.unset(guildId, logChannelType)
        logChannelCache.put(Pair(guildId, logChannelType), CompletableFuture.completedFuture(-1))
    }

    fun removeChannels(guildId: Long, logChannelTypes: List<LogChannelType>) {
        logChannelDao.bulkRemove(guildId, logChannelTypes)
        for (type in logChannelTypes) {
            logChannelCache.put(Pair(guildId, type), CompletableFuture.completedFuture(-1))
        }
    }

    fun setChannels(guildId: Long, logChannelTypes: List<LogChannelType>, channelId: Long) {
        logChannelDao.bulkPut(guildId, logChannelTypes, channelId)
        for (type in logChannelTypes) {
            logChannelCache.put(Pair(guildId, type), CompletableFuture.completedFuture(channelId))
        }
    }

    suspend fun setChannel(guildId: Long, logChannelType: LogChannelType, channelId: Long) {
        logChannelDao.set(guildId, logChannelType, channelId)
        logChannelCache.put(Pair(guildId, logChannelType), CompletableFuture.completedFuture(channelId))
    }
}