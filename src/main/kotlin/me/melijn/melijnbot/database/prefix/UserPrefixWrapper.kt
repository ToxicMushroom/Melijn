package me.melijn.melijnbot.database.prefix

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class UserPrefixWrapper(private val taskManager: TaskManager, private val userPrefixDao: UserPrefixDao) {

    val prefixCache = CacheBuilder.newBuilder()
        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, List<String>> { key ->
            getPrefixes(key)
        })

    private fun getPrefixes(userId: Long): CompletableFuture<List<String>> {
        val prefixes = CompletableFuture<List<String>>()
        taskManager.async {
            val prefixesString = userPrefixDao.get(userId)
            val list: List<String> = if (prefixesString == "") emptyList() else prefixesString.split("%SPLIT%")
            prefixes.complete(list)
        }
        return prefixes
    }

    suspend fun addPrefix(userId: Long, prefix: String) {
        val prefixList = prefixCache.get(userId).await().toMutableList()
        if (!prefixList.contains(prefix))
            prefixList.add(prefix)
        setPrefixes(userId, prefixList)
    }

    private suspend fun setPrefixes(userId: Long, prefixList: List<String>) {
        val prefixes = prefixList.joinToString("%SPLIT%")
        userPrefixDao.set(userId, prefixes)
        prefixCache.put(userId, CompletableFuture.completedFuture(prefixList))
    }

    suspend fun removePrefix(userId: Long, prefix: String) {
        val prefixList = prefixCache.get(userId).await().toMutableList()
        prefixList.remove(prefix)
        setPrefixes(userId, prefixList)
    }
}