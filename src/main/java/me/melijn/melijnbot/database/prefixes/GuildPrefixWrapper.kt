package me.melijn.melijnbot.database.prefixes

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class GuildPrefixWrapper(private val taskManager: TaskManager, private val guildPrefixDao: GuildPrefixDao) {

    val prefixCache = Caffeine.newBuilder()
            .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.getExecutorService())
            .buildAsync<Long, List<String>>() { key, executor -> getPrefixes(key, executor) }

    fun getPrefixes(guildId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<List<String>> {
        val prefixes = CompletableFuture<List<String>>()
        executor.execute {
            guildPrefixDao.get(guildId, Consumer { prefixesString ->
                val list: List<String> = if (prefixesString == "") listOf() else prefixesString.split("%SPLIT%")
                prefixes.complete(list)
            })
        }
        return prefixes
    }

    fun addPrefix(guildId: Long, prefix: String) {
        taskManager.async(Runnable {
            val prefixList = prefixCache.get(guildId).get()
            val prefixes = if (prefixList.isNotEmpty())
                "${prefixList.joinToString("%SPLIT%")}%SPLIT%$prefix"
            else prefix
            guildPrefixDao.set(guildId, prefixes)
        })
    }

    fun setPrefixes(guildId: Long, prefixList: List<String>) {
        taskManager.async(Runnable {
            val prefixes = prefixList.joinToString("%SPLIT%")
            guildPrefixDao.set(guildId, prefixes)
        })
    }
}