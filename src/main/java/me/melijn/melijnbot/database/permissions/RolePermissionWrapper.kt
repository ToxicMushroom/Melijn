package me.melijn.melijnbot.database.permissions

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class RolePermissionWrapper(val taskManager: TaskManager, val rolePermissionDao: RolePermissionDao) {
    val rolePermissionCache = Caffeine.newBuilder()
            .executor(taskManager.getExecutorService())
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .buildAsync<Long, Map<String, PermState>>() { key, executor -> getPermissionList(key, executor) }

    fun getPermissionList(roleId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Map<String, PermState>> {
        val languageFuture = CompletableFuture<Map<String, PermState>>()
        executor.execute {
            rolePermissionDao.getMap(roleId, Consumer { map ->
                languageFuture.complete(map)
            })
        }
        return languageFuture
    }
}