package me.melijn.melijnbot.database.embed

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class EmbedColorWrapper(private val embedColorDao: EmbedColorDao) {

    val embedColorCache = CacheBuilder.newBuilder()
        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Int> { key ->
            getColor(key)
        })

    private fun getColor(guildId: Long): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
       TaskManager.async {
            val int = embedColorDao.get(guildId)
            future.complete(int)
        }

        return future
    }

    suspend fun setColor(guildId: Long, color: Int) {
        embedColorCache.put(guildId, CompletableFuture.completedFuture(color))
        embedColorDao.set(guildId, color)
    }

    suspend fun removeColor(userId: Long) {
        embedColorCache.put(userId, CompletableFuture.completedFuture(0))
        embedColorDao.remove(userId)
    }
}