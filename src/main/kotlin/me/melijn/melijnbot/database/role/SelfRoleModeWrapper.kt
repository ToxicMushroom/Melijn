package me.melijn.melijnbot.database.role

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class SelfRoleModeWrapper(val taskManager: TaskManager, private val selfRoleModeDao: SelfRoleModeDao) {

    val selfRoleModeCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, SelfRoleMode> { key ->
            getMap(key)
        })

    fun getMap(guildId: Long): CompletableFuture<SelfRoleMode> {
        val future = CompletableFuture<SelfRoleMode>()
        taskManager.async {
            val map = selfRoleModeDao.getMode(guildId)
            future.complete(SelfRoleMode.valueOf(map))
        }
        return future
    }

    suspend fun set(guildId: Long, mode: SelfRoleMode) {
        if (mode == SelfRoleMode.AUTO) {
            selfRoleModeDao.delete(guildId)
            selfRoleModeCache.put(guildId, CompletableFuture.completedFuture(mode))
        } else {
            selfRoleModeDao.setMode(guildId, mode.toString())
            selfRoleModeCache.put(guildId, CompletableFuture.completedFuture(mode))
        }
    }
}