package me.melijn.melijnbot.database.role

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class RoleWrapper(private val taskManager: TaskManager, private val roleDao: RoleDao) {

    val roleCache = Caffeine.newBuilder()
            .executor(taskManager.getExecutorService())
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .buildAsync<Pair<Long, RoleType>, Long>() { key, executor -> getRoleId(key.first, key.second, executor) }

    private fun getRoleId(guildId: Long, roleType: RoleType, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()
        executor.execute {
            roleDao.get(guildId, roleType) {
                future.complete(it)
            }
        }
        return future
    }

    fun removeRole(guildId: Long, roleType: RoleType) {
        roleDao.unset(guildId, roleType)
        roleCache.put(Pair(guildId, roleType), CompletableFuture.completedFuture(-1))
    }

    fun setRole(guildId: Long, roleType: RoleType, channelId: Long) {
        roleDao.set(guildId, roleType, channelId)
        roleCache.put(Pair(guildId, roleType), CompletableFuture.completedFuture(channelId))
    }
}