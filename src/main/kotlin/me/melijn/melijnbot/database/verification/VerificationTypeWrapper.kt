package me.melijn.melijnbot.database.verification

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.enums.VerificationType
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class VerificationTypeWrapper(val taskManager: TaskManager, private val verificationTypeDao: VerificationTypeDao) {

    val verificationTypeCache = Caffeine.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Long, VerificationType>() { key, executor -> getType(key, executor) }

    private fun getType(guildId: Long, executor: Executor): CompletableFuture<VerificationType> {
        val future = CompletableFuture<VerificationType>()
        executor.launch {
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