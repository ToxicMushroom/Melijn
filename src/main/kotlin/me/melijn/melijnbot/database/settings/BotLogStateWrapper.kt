package me.melijn.melijnbot.database.settings

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class BotLogStateWrapper(

    private val botLogStateDao: BotLogStateDao
) {

    // true will log bot stuff
    val botLogStateCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Boolean> { key ->
            shouldLog(key)
        })

    private fun shouldLog(guildId: Long): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
       TaskManager.async {
            val state = botLogStateDao.contains(guildId)
            future.complete(state)
        }
        return future
    }

    suspend fun set(guildId: Long, state: Boolean) {
        if (state) {
            botLogStateDao.add(guildId)
        } else {
            botLogStateDao.remove(guildId)
        }

        botLogStateCache.put(guildId, CompletableFuture.completedFuture(state))
    }
}