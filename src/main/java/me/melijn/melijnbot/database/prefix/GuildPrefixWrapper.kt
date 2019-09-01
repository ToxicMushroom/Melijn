package me.melijn.melijnbot.database.prefix

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class GuildPrefixWrapper(private val taskManager: TaskManager, private val guildPrefixDao: GuildPrefixDao) {

    val prefixCache = Caffeine.newBuilder()
            .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.executorService)
            .buildAsync<Long, List<String>>() { key, executor -> getPrefixes(key, executor) }

    fun getPrefixes(guildId: Long, executor: Executor = taskManager.executorService): CompletableFuture<List<String>> {
        val prefixes = CompletableFuture<List<String>>()
        executor.execute {
            guildPrefixDao.get(guildId) { prefixesString ->
                val list: List<String> =
                        if (prefixesString == "") emptyList()
                        else prefixesString.split("%SPLIT%")
                prefixes.complete(list)
            }
        }
        return prefixes
    }

    fun addPrefix(guildId: Long, prefix: String) {
        val prefixList = prefixCache.get(guildId).get().toMutableList()
        if (!prefixList.contains(prefix))
            prefixList.add(prefix)
        setPrefixes(guildId, prefixList)
    }

    private fun setPrefixes(guildId: Long, prefixList: List<String>) {
        val prefixes = prefixList.joinToString("%SPLIT%")
        guildPrefixDao.set(guildId, prefixes)
        prefixCache.put(guildId, CompletableFuture.completedFuture(prefixList))
    }

    fun removePrefix(guildId: Long, prefix: String) {
        val prefixList = prefixCache.get(guildId).get().toMutableList()
        prefixList.remove(prefix)
        setPrefixes(guildId, prefixList)
    }
}