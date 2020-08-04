package me.melijn.melijnbot.database.channel

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ChannelWrapper(private val channelDao: ChannelDao) {

    val channelCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Pair<Long, ChannelType>, Long> { (first, second) ->
            getChannelId(first, second)
        })

    private fun getChannelId(guildId: Long, channelType: ChannelType): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()

       TaskManager.async {
            val channelId = channelDao.get(guildId, channelType)
            future.complete(channelId)
        }

        return future
    }

    suspend fun removeChannel(guildId: Long, channelType: ChannelType) {
        channelDao.remove(guildId, channelType)
        channelCache.put(Pair(guildId, channelType), CompletableFuture.completedFuture(-1))
    }

    suspend fun setChannel(guildId: Long, channelType: ChannelType, channelId: Long) {
        channelDao.set(guildId, channelType, channelId)
        channelCache.put(Pair(guildId, channelType), CompletableFuture.completedFuture(channelId))
    }

    suspend fun getChannels(channelType: ChannelType): Map<Long, Long> {
        return channelDao.getChannels(channelType)
    }
}