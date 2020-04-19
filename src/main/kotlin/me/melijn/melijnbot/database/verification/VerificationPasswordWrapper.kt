package me.melijn.melijnbot.database.verification

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class VerificationPasswordWrapper(val taskManager: TaskManager, private val verificationPasswordDao: VerificationPasswordDao) {

    val verificationPasswordCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, String> { key ->
            getCode(key)
        })

    private fun getCode(guildId: Long): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        taskManager.async {
            val password = verificationPasswordDao.get(guildId)
            future.complete(password)
        }
        return future
    }

    suspend fun set(guildId: Long, password: String) {
        verificationPasswordCache.put(guildId, CompletableFuture.completedFuture(password))
        verificationPasswordDao.set(guildId, password)
    }

    suspend fun remove(guildId: Long) {
        verificationPasswordCache.put(guildId, CompletableFuture.completedFuture(""))
        verificationPasswordDao.remove(guildId)
    }

}