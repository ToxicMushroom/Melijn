package me.melijn.melijnbot.database.verification

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class VerificationCodeWrapper(val taskManager: TaskManager, private val verificationCodeDao: VerificationCodeDao) {

    val verificationCodeCache = Caffeine.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Long, String>() { key, executor -> getCode(key, executor) }

    private fun getCode(guildId: Long, executor: Executor): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        executor.launch {
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