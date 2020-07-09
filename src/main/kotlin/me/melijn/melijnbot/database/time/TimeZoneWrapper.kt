package me.melijn.melijnbot.database.time

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class TimeZoneWrapper(private val taskManager: TaskManager, private val timeZoneDao: TimeZoneDao) {

    val timeZoneCache = CacheBuilder.newBuilder()
        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, String> { key ->
            getTimeZone(key)
        })

    private fun getTimeZone(id: Long): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        taskManager.async {
            val roleId = timeZoneDao.getZoneId(id)
            future.complete(roleId)
        }
        return future
    }

    suspend fun removeTimeZone(id: Long) {
        timeZoneDao.remove(id)
        timeZoneCache.put(id, CompletableFuture.completedFuture(""))
    }

    suspend fun setTimeZone(id: Long, zone: TimeZone) {
        timeZoneDao.put(id, zone.id)
        timeZoneCache.put(id, CompletableFuture.completedFuture(zone.id))
    }
}