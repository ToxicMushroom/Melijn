package me.melijn.melijnbot.database.role

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class RoleWrapper(private val taskManager: TaskManager, private val roleDao: RoleDao) {

    val roleCache = Caffeine.newBuilder()
        .executor(taskManager.executorService)
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .buildAsync<Pair<Long, RoleType>, Long>() { key, executor -> getRoleId(key.first, key.second, executor) }

    private fun getRoleId(guildId: Long, roleType: RoleType, executor: Executor = taskManager.executorService): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()
        executor.launch {
            val roleId = roleDao.get(guildId, roleType)
            future.complete(roleId)
        }
        return future
    }

    suspend fun removeRole(guildId: Long, roleType: RoleType) {
        roleDao.unset(guildId, roleType)
        roleCache.put(Pair(guildId, roleType), CompletableFuture.completedFuture(-1))
    }

    suspend fun setRole(guildId: Long, roleType: RoleType, channelId: Long) {
        roleDao.set(guildId, roleType, channelId)
        roleCache.put(Pair(guildId, roleType), CompletableFuture.completedFuture(channelId))
    }
}