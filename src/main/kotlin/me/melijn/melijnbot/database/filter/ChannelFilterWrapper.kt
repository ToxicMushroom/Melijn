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

class ChannelFilterWrapper (val taskManager: TaskManager, private val channelFilterDao: ChannelFilterDao) {

    val allowedChannelFilterCache = Caffeine.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Pair<Long, Long>, List<String>>() { key, executor -> getFilters(key.first, key.second, FilterType.ALLOWED, executor) }

    val deniedChannelFilterCache = Caffeine.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Pair<Long, Long>, List<String>>() { key, executor -> getFilters(key.first, key.second, FilterType.DENIED, executor) }

    private fun getFilters(guildId: Long, channelId: Long, filterType: FilterType, executor: Executor = taskManager.executorService): CompletableFuture<List<String>> {
        val future = CompletableFuture<List<String>>()
        executor.launch {
            val filters = channelFilterDao.get(guildId, channelId, filterType)
            future.complete(filters)
        }
        return future
    }

    suspend fun addFilter(guildId: Long, channelId: Long, filterType: FilterType, filter: String) {
        channelFilterDao.add(guildId, channelId, filterType, filter)

        val pair = Pair(guildId, channelId)
        when (filterType) {
            FilterType.ALLOWED -> {
                val newFilters = allowedChannelFilterCache.get(pair).await().toMutableList() + filter
                allowedChannelFilterCache.put(pair, CompletableFuture.completedFuture(newFilters))
            }
            FilterType.DENIED -> {
                val newFilters = deniedChannelFilterCache.get(pair).await().toMutableList() + filter
                deniedChannelFilterCache.put(pair, CompletableFuture.completedFuture(newFilters))
            }
        }
    }
}