package me.melijn.melijnbot.database.settings

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class RemoveResponseWrapper(

    private val removeResponsesDao: RemoveResponsesDao
) {

    val removeResponseCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Map<Long, Int>> {
            getMap(it)
        })

    private fun getMap(guildId: Long): CompletableFuture<Map<Long, Int>> {
        val future = CompletableFuture<Map<Long, Int>>()
       TaskManager.async {
            val result = removeResponsesDao.getChannels(guildId)
            future.complete(result)
        }
        return future
    }

    suspend fun set(guildId: Long, channelId: Long, seconds: Int) {
        val ls = removeResponseCache.get(guildId).await().toMutableMap()

        if (ls[channelId] != seconds) {
            ls[channelId] = seconds
            removeResponsesDao.insert(guildId, channelId, seconds)
            removeResponseCache.put(guildId, CompletableFuture.completedFuture(ls))
        }
    }

    suspend fun remove(guildId: Long, channelId: Long) {
        val ls = removeResponseCache.get(guildId).await().toMutableMap()

        if (ls.containsKey(channelId)) {
            ls.remove(channelId)
            removeResponsesDao.remove(guildId, channelId)
            removeResponseCache.put(guildId, CompletableFuture.completedFuture(ls))
        }
    }
}