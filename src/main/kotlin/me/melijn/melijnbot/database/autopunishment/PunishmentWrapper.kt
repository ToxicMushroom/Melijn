package me.melijn.melijnbot.database.autopunishment

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class PunishmentWrapper(val taskManager: TaskManager, val punishmentDao: PunishmentDao) {

    val punishmentCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, List<Punishment>> { guildId ->
            getList(guildId)
        })

    private fun getList(guildId: Long): CompletableFuture<List<Punishment>> {
        val future = CompletableFuture<List<Punishment>>()
        taskManager.async {
            val map = punishmentDao.get(guildId)
            future.complete(map)
        }
        return future
    }

    suspend fun put(guildId: Long, punishment: Punishment) {
        punishmentDao.put(guildId, punishment)
        val list = punishmentCache.get(guildId).await().toMutableList()
        list.removeIf { (pName) ->
            pName == punishment.name
        }
        list.add(punishment)
        punishmentCache.put(guildId, CompletableFuture.completedFuture(list))
    }

    suspend fun remove(guildId: Long, name: String) {
        punishmentDao.remove(guildId, name)
        punishmentCache.put(guildId, CompletableFuture.completedFuture(
            punishmentCache.get(guildId).await().toMutableList().filter { (pName) ->
                pName != name
            }
        ))
    }
}