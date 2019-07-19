package me.melijn.melijnbot.database.permissions

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class ChannelRolePermissionWrapper(val taskManager: TaskManager, private val channelRolePermissionDao: ChannelRolePermissionDao) {
    val channelRolePermissionCache = Caffeine.newBuilder()
            .executor(taskManager.getExecutorService())
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .buildAsync<Pair<Long, Long>, Map<String, PermState>>() { pair, executor -> getPermissionList(pair.first, pair.second, executor) }

    fun getPermissionList(channelId: Long, roleId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Map<String, PermState>> {
        val languageFuture = CompletableFuture<Map<String, PermState>>()
        executor.execute {
            channelRolePermissionDao.getMap(channelId, roleId, Consumer { map ->
                languageFuture.complete(map)
            })
        }
        return languageFuture
    }
}