package me.melijn.melijnbot.database.language

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.enums.Language
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class GuildLanguageWrapper(private val taskManager: TaskManager, private val languageDao: GuildLanguageDao) {

    val languageCache = CacheBuilder.newBuilder()
        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, String> { key ->
            getLanguage(key)
        })

    private fun getLanguage(guildId: Long): CompletableFuture<String> {
        val languageFuture = CompletableFuture<String>()
        taskManager.async {
            val language = languageDao.get(guildId)
            languageFuture.complete(language)
        }
        return languageFuture
    }

    suspend fun setLanguage(guildId: Long, language: String) {
        val future = CompletableFuture<String>()
        languageCache.put(guildId, future)

        if (language.isEmpty()) {
            future.complete(Language.EN.toString())
            languageDao.remove(guildId)
        } else {
            future.complete(language)
            languageDao.set(guildId, language)
        }
    }
}