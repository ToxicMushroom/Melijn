package me.melijn.melijnbot.database.role

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class JoinRoleGroupWrapper(val taskManager: TaskManager, private val joinRoleGroupDao: JoinRoleGroupDao) {

    val joinRoleGroupCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, List<JoinRoleGroupInfo>> { key ->
            getList(key)
        })

    private fun getList(guildId: Long): CompletableFuture<List<JoinRoleGroupInfo>> {
        val future = CompletableFuture<List<JoinRoleGroupInfo>>()
        taskManager.async {
            val list = joinRoleGroupDao.get(guildId)
            future.complete(list)
        }
        return future
    }

    suspend fun insertOrUpdate(guildId: Long, selfRoleGroup: JoinRoleGroupInfo) {
        val list = joinRoleGroupCache.get(guildId).await().toMutableList()
        list.firstOrNull { (groupName) ->
            groupName == selfRoleGroup.groupName
        }?.let { group ->
            list.remove(group)
        }

        list.add(selfRoleGroup)

        joinRoleGroupDao.put(guildId, selfRoleGroup)
        joinRoleGroupCache.put(guildId, CompletableFuture.completedFuture(list))
    }

    suspend fun delete(guildId: Long, groupName1: String) {
        val list = joinRoleGroupCache.get(guildId).await().toMutableList()
        list.firstOrNull { (groupName) ->
            groupName == groupName1
        }?.let { group ->
            list.remove(group)
        }

        joinRoleGroupDao.remove(guildId)
        joinRoleGroupCache.put(guildId, CompletableFuture.completedFuture(list))
    }
}