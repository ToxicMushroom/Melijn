package me.melijn.melijnbot.database.language

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class UserLanguageWrapper(val taskManager: TaskManager, private val userLanguageDao: UserLanguageDao) {
    val languageCache = Caffeine.newBuilder()
        .executor(taskManager.executorService)
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .buildAsync<Long, String>() { key, executor -> getLanguage(key, executor) }

    private fun getLanguage(userId: Long, executor: Executor = taskManager.executorService): CompletableFuture<String> {
        val languageFuture = CompletableFuture<String>()
        executor.launch {
            val language = userLanguageDao.get(userId)
            languageFuture.complete(language)
        }
        return languageFuture
    }

    suspend fun setLanguage(guildId: Long, language: String) {
        val future = CompletableFuture.completedFuture(language)
        languageCache.put(guildId, future)
        userLanguageDao.set(guildId, language)
    }
}