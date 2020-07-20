package me.melijn.melijnbot.database.channel

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class StreamUrlWrapper(private val streamUrlDao: StreamUrlDao) {

    val streamUrlCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, String> { key ->
            getCode(key)
        })

    private fun getCode(guildId: Long): CompletableFuture<String> {
        val future = CompletableFuture<String>()
       TaskManager.async {
            val url = streamUrlDao.get(guildId)
            future.complete(url)
        }
        return future
    }

    suspend fun setUrl(guildId: Long, url: String) {
        streamUrlCache.put(guildId, CompletableFuture.completedFuture(url))
        streamUrlDao.set(guildId, url)
    }

    suspend fun removeUrl(guildId: Long) {
        streamUrlCache.put(guildId, CompletableFuture.completedFuture(""))
        streamUrlDao.remove(guildId)
    }

}