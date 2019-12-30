package me.melijn.melijnbot.database.filter

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class FilterGroupWrapper(val taskManager: TaskManager, val filterGroupDao: FilterGroupDao) {

    val filterGroupCache = CacheBuilder.newBuilder()
        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, List<FilterGroup>> { guildId ->
            getGroups(guildId)
        })

    private fun getGroups(guildId: Long): CompletableFuture<List<FilterGroup>> {
        val future = CompletableFuture<List<FilterGroup>>()
        taskManager.async {
            val mode = filterGroupDao.get(guildId)
            future.complete(mode)
        }
        return future
    }

    suspend fun putGroup(guildId: Long, group: FilterGroup) {
        val list = filterGroupCache.get(guildId).await().toMutableList()
        list.removeIf { (groupId) -> groupId == group.filterGroupName }
        list.add(group)
        filterGroupCache.put(guildId, CompletableFuture.completedFuture(list))
        filterGroupDao.add(guildId, group)
    }

    suspend fun deleteGroup(guildId: Long, group: FilterGroup) {
        val list = filterGroupCache.get(guildId).await().toMutableList()
        list.removeIf { (groupId) -> groupId == group.filterGroupName }
        filterGroupCache.put(guildId, CompletableFuture.completedFuture(list))
        filterGroupDao.remove(guildId, group)
    }
}