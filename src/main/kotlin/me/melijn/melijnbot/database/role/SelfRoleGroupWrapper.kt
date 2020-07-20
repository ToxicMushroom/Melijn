package me.melijn.melijnbot.database.role

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class SelfRoleGroupWrapper(private val selfRoleGroupDao: SelfRoleGroupDao) {

    val selfRoleGroupCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, List<SelfRoleGroup>> { key ->
            getMap(key)
        })

    fun getMap(guildId: Long): CompletableFuture<List<SelfRoleGroup>> {
        val future = CompletableFuture<List<SelfRoleGroup>>()
       TaskManager.async {
            val map = selfRoleGroupDao.get(guildId)
            future.complete(map)
        }
        return future
    }

    suspend fun insertOrUpdate(guildId: Long, selfRoleGroup: SelfRoleGroup) {
        val list = selfRoleGroupCache.get(guildId).await().toMutableList()
        list.firstOrNull { (groupName) ->
            groupName == selfRoleGroup.groupName
        }?.let { group ->
            list.remove(group)
        }

        list.add(selfRoleGroup)

        selfRoleGroupDao.set(guildId, selfRoleGroup.groupName, selfRoleGroup.messageIds.joinToString(), selfRoleGroup.channelId, selfRoleGroup.isEnabled, selfRoleGroup.pattern
            ?: "", selfRoleGroup.isSelfRoleable)
        selfRoleGroupCache.put(guildId, CompletableFuture.completedFuture(list))
    }

    suspend fun delete(guildId: Long, groupName1: String) {
        val list = selfRoleGroupCache.get(guildId).await().toMutableList()
        list.firstOrNull { (groupName) ->
            groupName == groupName1
        }?.let { group ->
            list.remove(group)
        }

        selfRoleGroupDao.remove(guildId, groupName1)
        selfRoleGroupCache.put(guildId, CompletableFuture.completedFuture(list))
    }
}