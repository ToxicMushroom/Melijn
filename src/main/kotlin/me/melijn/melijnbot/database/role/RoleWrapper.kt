package me.melijn.melijnbot.database.role

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class RoleWrapper(private val roleDao: RoleDao) {

    val roleCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Pair<Long, RoleType>, Long> { (first, second) ->
            getRoleId(first, second)
        })

    private fun getRoleId(guildId: Long, roleType: RoleType): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()
       TaskManager.async {
            val roleId = roleDao.get(guildId, roleType)
            future.complete(roleId)
        }
        return future
    }

    suspend fun removeRole(guildId: Long, roleType: RoleType) {
        roleDao.unset(guildId, roleType)
        roleCache.put(Pair(guildId, roleType), CompletableFuture.completedFuture(-1))
    }

    suspend fun setRole(guildId: Long, roleType: RoleType, roleId: Long) {
        roleDao.set(guildId, roleType, roleId)
        roleCache.put(Pair(guildId, roleType), CompletableFuture.completedFuture(roleId))
    }

    //guildId -> roleId
    suspend fun getRoles(birthday: RoleType): Map<Long, Long> {
        return roleDao.getRoles(birthday)
    }
}