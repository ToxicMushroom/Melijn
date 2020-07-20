package me.melijn.melijnbot.database.settings

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class RemoveInvokeWrapper(

    private val removeInvokeDao: RemoveInvokeDao
) {

    val removeInvokeCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Map<Long, Int>> {
            getMap(it)
        })

    private fun getMap(guildId: Long): CompletableFuture<Map<Long, Int>> {
        val future = CompletableFuture<Map<Long, Int>>()
       TaskManager.async {
            val result = removeInvokeDao.getChannels(guildId)
            future.complete(result)
        }
        return future
    }

    suspend fun set(guildId: Long, channelId: Long, seconds: Int) {
        val ls = removeInvokeCache.get(guildId).await().toMutableMap()

        if (ls[channelId] != seconds) {
            ls[channelId] = seconds
            removeInvokeDao.insert(guildId, channelId, seconds)
            removeInvokeCache.put(guildId, CompletableFuture.completedFuture(ls))
        }
    }

    suspend fun remove(guildId: Long, channelId: Long) {
        val ls = removeInvokeCache.get(guildId).await().toMutableMap()

        if (ls.containsKey(channelId)) {
            ls.remove(channelId)
            removeInvokeDao.remove(guildId, channelId)
            removeInvokeCache.put(guildId, CompletableFuture.completedFuture(ls))
        }
    }
}