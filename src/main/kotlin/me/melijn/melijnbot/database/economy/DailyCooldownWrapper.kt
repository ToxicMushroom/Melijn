package me.melijn.melijnbot.database.economy

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class DailyCooldownWrapper(private val dailyCooldownDao: DailyCooldownDao) {

    val cooldownCache = CacheBuilder.newBuilder()
        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Long> { key ->
            getCooldown(key)
        })

    private fun getCooldown(userId: Long): CompletableFuture<Long> {
        val time = CompletableFuture<Long>()
       TaskManager.async {
            time.complete(dailyCooldownDao.get(userId))

        }
        return time
    }

    suspend fun setCooldown(userId: Long, time: Long) {
        dailyCooldownDao.set(userId, time)
        cooldownCache.put(userId, CompletableFuture.completedFuture(time))
    }
}