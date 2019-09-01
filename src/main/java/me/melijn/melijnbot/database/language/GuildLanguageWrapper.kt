package me.melijn.melijnbot.database.language

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.enums.Language
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class GuildLanguageWrapper(private val taskManager: TaskManager, private val languageDao: GuildLanguageDao) {

    val languageCache = Caffeine.newBuilder()
            .executor(taskManager.executorService)
            .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
            .buildAsync<Long, String>() { key, executor -> getLanguage(key, executor) }

    private fun getLanguage(guildId: Long, executor: Executor = taskManager.executorService): CompletableFuture<String> {
        val languageFuture = CompletableFuture<String>()
        executor.execute {
            languageDao.get(guildId) { language ->
                languageFuture.complete(language)
            }
        }
        return languageFuture
    }

    fun setLanguage(guildId: Long, language: String) {
        val future = CompletableFuture<String>()
        languageCache.put(guildId, future)

        if ("".equals(language, true)) {
            future.complete(Language.EN.toString())
            languageDao.remove(guildId)
        } else {
            future.complete(language)
            languageDao.set(guildId, language)
        }
    }
}