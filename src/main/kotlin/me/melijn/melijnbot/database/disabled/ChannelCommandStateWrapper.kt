package me.melijn.melijnbot.database.disabled

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.ChannelCommandState
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ChannelCommandStateWrapper(val taskManager: TaskManager, private val channelCommandStateDao: ChannelCommandStateDao) {

    val channelCommandsStateCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Map<String, ChannelCommandState>> { key ->
            getCommandStateMap(key)
        })

    private fun getCommandStateMap(channelId: Long): CompletableFuture<Map<String, ChannelCommandState>> {
        val future = CompletableFuture<Map<String, ChannelCommandState>>()
        taskManager.async {
            val id = channelCommandStateDao.get(channelId)
            future.complete(id)
        }
        return future
    }

    suspend fun setCommandState(guildId: Long, channelId: Long, commandIds: Set<String>, channelCommandState: ChannelCommandState) {
        val map = channelCommandsStateCache.get(channelId).await().toMutableMap()
        if (channelCommandState == ChannelCommandState.DEFAULT) {
            channelCommandStateDao.bulkRemove(channelId, commandIds)
            for (id in commandIds) {
                map.remove(id)
            }
        } else {
            channelCommandStateDao.bulkPut(guildId, channelId, commandIds, channelCommandState)
            for (id in commandIds) {
                map[id] = channelCommandState
            }
        }
        channelCommandsStateCache.put(channelId, CompletableFuture.completedFuture(map.toMap()))
    }
}