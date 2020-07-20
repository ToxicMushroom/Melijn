package me.melijn.melijnbot.database.language

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class UserLanguageWrapper(private val userLanguageDao: UserLanguageDao) {

    val languageCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, String> { key ->
            getLanguage(key)
        })

    private fun getLanguage(userId: Long): CompletableFuture<String> {
        val languageFuture = CompletableFuture<String>()
       TaskManager.async {
            val language = userLanguageDao.get(userId)
            languageFuture.complete(language)
        }
        return languageFuture
    }

    suspend fun setLanguage(userId: Long, language: String) {
        val future = CompletableFuture.completedFuture(language)
        languageCache.put(userId, future)
        userLanguageDao.set(userId, language)
    }

    suspend fun removeLanguage(authorId: Long) {
        languageCache.put(authorId, CompletableFuture.completedFuture(""))
        userLanguageDao.remove(authorId)
    }
}