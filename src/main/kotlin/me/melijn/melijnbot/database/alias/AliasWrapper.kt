package me.melijn.melijnbot.database.alias

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.addIfNotPresent
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class AliasWrapper(val taskManager: TaskManager, private val aliasDao: AliasDao) {


    val aliasCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Map<String, List<String>>> { id ->
            getAliases(id)
        })

    private fun getAliases(id: Long): CompletableFuture<Map<String, List<String>>> {
        val future = CompletableFuture<Map<String, List<String>>>()

        taskManager.async {
            val profileMap = aliasDao.getAliases(id)
            future.complete(profileMap)
        }

        return future
    }

    suspend fun add(id: Long, command: String, alias: String) {
        val map = aliasCache[id].await().toMutableMap()
        val newList = ((map[command]?.toMutableList() ?: mutableListOf()))
        val added = newList.addIfNotPresent(alias, true)
        if (added) {

            map[command] = newList
            aliasCache.put(id, CompletableFuture.completedFuture(map))
            aliasDao.insert(id, command, newList.joinToString("%SPLIT%"))
        }
    }

    suspend fun remove(id: Long, command: String, alias: String) {
        val map = aliasCache[id].await().toMutableMap()
        val newList = ((map[command]?.toMutableList() ?: mutableListOf()))

        val removed = newList.removeIf { it.equals(alias, true) }
        if (removed) {

            map[command] = newList

            if (newList.isEmpty()) {
                map.remove(command)
                aliasDao.remove(id, command)
            } else {
                aliasDao.insert(id, command, newList.joinToString("%SPLIT%"))
            }

            aliasCache.put(id, CompletableFuture.completedFuture(map))
        }
    }

    suspend fun clear(id: Long, command: String) {
        aliasDao.clear(id, command)
    }
}