package me.melijn.melijnbot.database.filter

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.FilterType
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.addIfNotPresent
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class FilterWrapper(private val filterDao: FilterDao) {

    //guildId, filterName, filter
    val allowedFilterCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Pair<Long, String>, List<String>> { (first, second) ->
            getFilters(first, second, FilterType.ALLOWED)
        })


    val deniedFilterCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Pair<Long, String>, List<String>> { (first, second) ->
            getFilters(first, second, FilterType.DENIED)
        })

    private fun getFilters(guildId: Long, filterGroupName: String, filterType: FilterType): CompletableFuture<List<String>> {
        val future = CompletableFuture<List<String>>()
        TaskManager.async {
            val filters = filterDao.get(guildId, filterGroupName, filterType)
            future.complete(filters)
        }
        return future
    }

    suspend fun addFilter(guildId: Long, filterGroupName: String, filterType: FilterType, filter: String) {
        filterDao.add(guildId, filterGroupName, filterType, filter)

        val pair = Pair(guildId, filterGroupName)

        when (filterType) {
            FilterType.ALLOWED -> {
                val filters = allowedFilterCache.get(pair).await().toMutableList()
                filters.addIfNotPresent(filter)
                allowedFilterCache.put(pair, CompletableFuture.completedFuture(filters))
            }
            FilterType.DENIED -> {
                val filters = deniedFilterCache.get(pair).await().toMutableList()
                filters.addIfNotPresent(filter)
                deniedFilterCache.put(pair, CompletableFuture.completedFuture(filters))
            }
        }
    }

    suspend fun removeFilter(guildId: Long, filterGroupName: String, type: FilterType, filter: String) {
        filterDao.remove(guildId, filterGroupName, type, filter)

        val pair = Pair(guildId, filterGroupName)
        when (type) {
            FilterType.ALLOWED -> {
                val newFilters = allowedFilterCache.get(pair).await().toMutableList() - filter
                allowedFilterCache.put(pair, CompletableFuture.completedFuture(newFilters))
            }
            FilterType.DENIED -> {
                val newFilters = deniedFilterCache.get(pair).await().toMutableList() - filter
                deniedFilterCache.put(pair, CompletableFuture.completedFuture(newFilters))
            }
        }
    }

    suspend fun contains(guildId: Long, filterGroupName: String, type: FilterType, filter: String): Boolean {
        val pair = Pair(guildId, filterGroupName)
        return when (type) {
            FilterType.ALLOWED -> allowedFilterCache
            FilterType.DENIED -> deniedFilterCache
        }.get(pair).await().contains(filter)
    }
}