package me.melijn.melijnbot.database.language

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class GuildLanguageWrapper(private val taskManager: TaskManager, private val languageDao: GuildLanguageDao) {

    val languageCache = Caffeine.newBuilder()
            .executor(taskManager.getExecutorService())
            .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
            .buildAsync<Long, String>() { key, executor -> getLanguage(key, executor) }

    fun getLanguage(guildId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<String> {
        val languageFuture = CompletableFuture<String>()
        executor.execute {
            languageDao.get(guildId, Consumer { language ->
                languageFuture.complete(language)
            })
        }
        return languageFuture
    }
}