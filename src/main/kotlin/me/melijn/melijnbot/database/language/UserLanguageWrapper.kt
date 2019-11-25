package me.melijn.melijnbot.database.language

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class UserLanguageWrapper(val taskManager: TaskManager, private val userLanguageDao: UserLanguageDao) {

    val languageCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, String> { key ->
            getLanguage(key)
        })

    private fun getLanguage(userId: Long): CompletableFuture<String> {
        val languageFuture = CompletableFuture<String>()
        taskManager.async {
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