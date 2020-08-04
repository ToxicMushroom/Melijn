package me.melijn.melijnbot.database.settings

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class BannedOrKickedTriggersLeaveWrapper(

    private val bannedOrKickedTriggersLeaveDao: BannedOrKickedTriggersLeaveDao
) {
    val bannedOrKickedTriggersLeaveCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Boolean> { key ->
            shouldTrigger(key)
        })

    private fun shouldTrigger(guildId: Long): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
       TaskManager.async {
            val state = bannedOrKickedTriggersLeaveDao.contains(guildId)
            future.complete(state)
        }
        return future
    }

    suspend fun set(guildId: Long, state: Boolean) {
        if (state) {
            bannedOrKickedTriggersLeaveDao.add(guildId)
        } else {
            bannedOrKickedTriggersLeaveDao.remove(guildId)
        }

        bannedOrKickedTriggersLeaveCache.put(guildId, CompletableFuture.completedFuture(state))
    }

}