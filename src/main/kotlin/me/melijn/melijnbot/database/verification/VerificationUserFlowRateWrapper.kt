package me.melijn.melijnbot.database.verification

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class VerificationUserFlowRateWrapper(private val verificationUserFlowRateDao: VerificationUserFlowRateDao) {

    val verificationUserFlowRateCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Long> { key ->
            getUserFlowRate(key)
        })

    private fun getUserFlowRate(guildId: Long): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()
       TaskManager.async {
            val flowRate = verificationUserFlowRateDao.get(guildId)
            future.complete(flowRate)
        }
        return future
    }

    suspend fun setUserFlowRate(guildId: Long, rate: Long) {
        verificationUserFlowRateCache.put(guildId, CompletableFuture.completedFuture(rate))
        verificationUserFlowRateDao.set(guildId, rate)
    }

    suspend fun removeFlowRate(guildId: Long) {
        verificationUserFlowRateCache.put(guildId, CompletableFuture.completedFuture(-1))
        verificationUserFlowRateDao.remove(guildId)
    }
}