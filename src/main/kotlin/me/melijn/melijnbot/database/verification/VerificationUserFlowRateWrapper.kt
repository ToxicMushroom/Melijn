package me.melijn.melijnbot.database.verification

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class VerificationUserFlowRateWrapper(val taskManager: TaskManager, private val verificationUserFlowRateDao: VerificationUserFlowRateDao) {
    val verificationUserFlowRateCache = Caffeine.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Long, Long>() { key, executor -> getUserFlowRate(key, executor) }

    private fun getUserFlowRate(guildId: Long, executor: Executor): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()
        executor.launch {
            val flowRate = verificationUserFlowRateDao.get(guildId)
            future.complete(flowRate)
        }
        return future
    }

    suspend fun setUserFlowRate(guildId: Long, rate: Long) {
        verificationUserFlowRateCache.put(guildId, CompletableFuture.completedFuture(rate))
        verificationUserFlowRateDao.set(guildId, rate)
    }

    suspend fun removeCode(guildId: Long) {
        verificationUserFlowRateCache.put(guildId, CompletableFuture.completedFuture(-1))
        verificationUserFlowRateDao.remove(guildId)
    }

}