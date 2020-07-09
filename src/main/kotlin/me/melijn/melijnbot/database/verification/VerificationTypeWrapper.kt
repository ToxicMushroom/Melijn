package me.melijn.melijnbot.database.verification

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.enums.VerificationType
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class VerificationTypeWrapper(val taskManager: TaskManager, private val verificationTypeDao: VerificationTypeDao) {

    val verificationTypeCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, VerificationType> { key ->
            getType(key)
        })

    private fun getType(guildId: Long): CompletableFuture<VerificationType> {
        val future = CompletableFuture<VerificationType>()
        taskManager.async {
            val type = verificationTypeDao.get(guildId)
            future.complete(type)
        }
        return future
    }

    suspend fun setType(guildId: Long, type: VerificationType) {
        verificationTypeCache.put(guildId, CompletableFuture.completedFuture(type))
        verificationTypeDao.set(guildId, type)
    }

    suspend fun removeType(guildId: Long) {
        verificationTypeCache.put(guildId, CompletableFuture.completedFuture(VerificationType.NONE))
        verificationTypeDao.remove(guildId)
    }

}