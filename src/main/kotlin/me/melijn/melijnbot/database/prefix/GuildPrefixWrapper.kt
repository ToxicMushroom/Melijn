package me.melijn.melijnbot.database.prefix

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class GuildPrefixWrapper(private val guildPrefixDao: GuildPrefixDao) {

    val prefixCache = CacheBuilder.newBuilder()
        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, List<String>> { key ->
            getPrefixes(key)
        })

    private fun getPrefixes(guildId: Long): CompletableFuture<List<String>> {
        val prefixes = CompletableFuture<List<String>>()
       TaskManager.async {
            val prefixesString = guildPrefixDao.get(guildId)
            val list: List<String> =
                if (prefixesString == "") emptyList()
                else prefixesString.split("%SPLIT%")
            prefixes.complete(list)

        }
        return prefixes
    }

    suspend fun addPrefix(guildId: Long, prefix: String) {
        val prefixList = prefixCache.get(guildId).get().toMutableList()
        if (!prefixList.contains(prefix))
            prefixList.add(prefix)
        setPrefixes(guildId, prefixList)
    }

    private suspend fun setPrefixes(guildId: Long, prefixList: List<String>) {
        val prefixes = prefixList.joinToString("%SPLIT%")
        guildPrefixDao.set(guildId, prefixes)
        prefixCache.put(guildId, CompletableFuture.completedFuture(prefixList))
    }

    suspend fun removePrefix(guildId: Long, prefix: String) {
        val prefixList = prefixCache.get(guildId).get().toMutableList()
        prefixList.remove(prefix)
        setPrefixes(guildId, prefixList)
    }
}