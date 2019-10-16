package me.melijn.melijnbot.database.verification

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class VerificationEmotejiWrapper(val taskManager: TaskManager, private val verificationEmotejiDao: VerificationEmotejiDao) {
    val verificationEmotejiCache = Caffeine.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Long, String>() { key, executor -> getEmoteji(key, executor) }

    private fun getEmoteji(guildId: Long, executor: Executor): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        executor.launch {
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