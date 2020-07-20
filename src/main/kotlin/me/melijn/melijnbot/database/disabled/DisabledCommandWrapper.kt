package me.melijn.melijnbot.database.disabled

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.CommandState
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class DisabledCommandWrapper(private val disabledCommandDao: DisabledCommandDao) {

    //guildId | commandId (or ccId)
    val disabledCommandsCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Set<String>> { key ->
            getDisabledCommandSet(key)
        })

    private fun getDisabledCommandSet(guildId: Long): CompletableFuture<Set<String>> {
        val future = CompletableFuture<Set<String>>()
       TaskManager.async {
            val id = disabledCommandDao.get(guildId)
            future.complete(id)
        }
        return future
    }

    suspend fun setCommandState(guildId: Long, commandIds: Set<String>, commandState: CommandState) {
        val set = disabledCommandsCache.get(guildId).await().toMutableSet()

        if (commandState == CommandState.DISABLED) {
            for (id in commandIds) {
                if (!set.contains(id))
                    set.add(id)
            }
            disabledCommandDao.bulkPut(guildId, commandIds)
        } else {
            for (id in commandIds) {
                set.remove(id)
            }
            disabledCommandDao.bulkDelete(guildId, commandIds)
        }
        disabledCommandsCache.put(guildId, CompletableFuture.completedFuture(set.toSet()))
    }
}