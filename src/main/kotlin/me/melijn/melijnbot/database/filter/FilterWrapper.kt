package me.melijn.melijnbot.database.filter

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.FilterType
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class FilterWrapper(val taskManager: TaskManager, private val filterDao: FilterDao) {

    val allowedFilterCache = Caffeine.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Long, List<String>>() { key, executor -> getFilters(key, FilterType.ALLOWED, executor) }

    val deniedFilterCache = Caffeine.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Long, List<String>>() { key, executor -> getFilters(key, FilterType.DENIED, executor) }

    private fun getFilters(guildId: Long, filterType: FilterType, executor: Executor = taskManager.executorService): CompletableFuture<List<String>> {
        val future = CompletableFuture<List<String>>()
        executor.launch {
            val filters = filterDao.get(guildId, filterType)
            future.complete(filters)
        }
        return future
    }

    suspend fun addFilter(guildId: Long, filterType: FilterType, filter: String) {
        filterDao.add(guildId, filterType, filter)

        when (filterType) {
            FilterType.ALLOWED -> {
                val newFilters = allowedFilterCache.get(guildId).await().toMutableList() + filter
                allowedFilterCache.put(guildId, CompletableFuture.completedFuture(newFilters))
            }
            FilterType.DENIED -> {
                val newFilters = deniedFilterCache.get(guildId).await().toMutableList() + filter
                deniedFilterCache.put(guildId, CompletableFuture.completedFuture(newFilters))
            }
        }
    }
}