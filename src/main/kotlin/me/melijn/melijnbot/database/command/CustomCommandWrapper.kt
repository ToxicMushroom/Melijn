package me.melijn.melijnbot.database.command

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


class CustomCommandWrapper(private val taskManager: TaskManager, private val customCommandDao: CustomCommandDao) {

    val customCommandCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.HOURS)
        .build(loadingCacheFrom<Long, List<CustomCommand>> { key ->
            getCustomCommands(key)
        })

    private fun getCustomCommands(guildId: Long): CompletableFuture<List<CustomCommand>> {
        val future = CompletableFuture<List<CustomCommand>>()

        taskManager.executorService.launch {
            val customCommands = customCommandDao.getForGuild(guildId)
            future.complete(customCommands)
        }

        return future
    }

    suspend fun add(guildId: Long, cc: CustomCommand): Long {
        val id = customCommandDao.add(guildId, cc)
        val list = customCommandCache.get(guildId).await().toMutableList()
        cc.id = id
        list.add(cc)
        customCommandCache.put(guildId, CompletableFuture.completedFuture(list))
        return id
    }

    suspend fun remove(guildId: Long, id: Long) {
        val list = customCommandCache.get(guildId).await().toMutableList()
        list.removeIf { it.id == id }
        customCommandCache.put(guildId, CompletableFuture.completedFuture(list))
        customCommandDao.remove(guildId, id)
    }

    suspend fun update(guildId: Long, cc: CustomCommand) {
        val list = customCommandCache.get(guildId).await().toMutableList()
        list.removeIf { it.id == cc.id }
        list.add(cc)
        customCommandCache.put(guildId, CompletableFuture.completedFuture(list))
        customCommandDao.update(guildId, cc)
    }

    suspend fun getCCById(guildId: Long, id: Long?): CustomCommand? {
        if (id == null) return null
        val list = customCommandCache.get(guildId).await()
        return list.firstOrNull { (id1) -> id1 == id }
    }
}