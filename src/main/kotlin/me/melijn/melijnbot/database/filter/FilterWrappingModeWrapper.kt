package me.melijn.melijnbot.database.filter

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.enums.FilterMode
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class FilterWrappingModeWrapper(val taskManager: TaskManager, private val filterWrappingModeDao: FilterWrappingModeDao) {

    val filterWrappingModeCache = CacheBuilder.newBuilder()
        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Pair<Long, Long?>, FilterMode> { (first, second) ->
            getMode(first, second)
        })

    private fun getMode(guildId: Long, channelId: Long?): CompletableFuture<FilterMode> {
        val future = CompletableFuture<FilterMode>()
        taskManager.async {
            val mode = filterWrappingModeDao.get(guildId, channelId)
            future.complete(mode)
        }
        return future
    }

    suspend fun setMode(guildId: Long, channelId: Long?, mode: FilterMode) {
        if (mode == FilterMode.DEFAULT && channelId == null) {
            unsetMode(guildId, channelId)
            return
        }
        filterWrappingModeCache.put(Pair(guildId, channelId), CompletableFuture.completedFuture(mode))
        filterWrappingModeDao.set(guildId, channelId, mode)
    }

    suspend fun unsetMode(guildId: Long, channelId: Long?) {
        filterWrappingModeCache.put(Pair(guildId, channelId), CompletableFuture.completedFuture(FilterMode.NO_MODE))
        filterWrappingModeDao.unset(guildId, channelId)
    }
}