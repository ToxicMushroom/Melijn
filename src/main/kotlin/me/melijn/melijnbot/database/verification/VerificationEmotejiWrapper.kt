package me.melijn.melijnbot.database.verification

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class VerificationEmotejiWrapper(private val verificationEmotejiDao: VerificationEmotejiDao) {

    val verificationEmotejiCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, String> { key ->
            getEmoteji(key)
        })

    private fun getEmoteji(guildId: Long): CompletableFuture<String> {
        val future = CompletableFuture<String>()
       TaskManager.async {
            val emoteji = verificationEmotejiDao.get(guildId)
            future.complete(emoteji)
        }
        return future
    }

    suspend fun setEmoteji(guildId: Long, emoteji: String) {
        verificationEmotejiCache.put(guildId, CompletableFuture.completedFuture(emoteji))
        verificationEmotejiDao.set(guildId, emoteji)
    }

    suspend fun removeEmoteji(guildId: Long) {
        verificationEmotejiCache.put(guildId, CompletableFuture.completedFuture(""))
        verificationEmotejiDao.remove(guildId)
    }

}