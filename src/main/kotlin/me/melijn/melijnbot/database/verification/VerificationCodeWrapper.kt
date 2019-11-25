package me.melijn.melijnbot.database.verification

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class VerificationCodeWrapper(val taskManager: TaskManager, private val verificationCodeDao: VerificationCodeDao) {

    val verificationCodeCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, String> { key ->
            getCode(key)
        })

    private fun getCode(guildId: Long): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        taskManager.async {
            val code = verificationCodeDao.get(guildId)
            future.complete(code)
        }
        return future
    }

    suspend fun setCode(guildId: Long, code: String) {
        verificationCodeCache.put(guildId, CompletableFuture.completedFuture(code))
        verificationCodeDao.set(guildId, code)
    }

    suspend fun removeCode(guildId: Long) {
        verificationCodeCache.put(guildId, CompletableFuture.completedFuture(""))
        verificationCodeDao.remove(guildId)
    }

}