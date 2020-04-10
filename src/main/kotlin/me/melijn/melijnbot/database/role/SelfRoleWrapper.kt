package me.melijn.melijnbot.database.role

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class SelfRoleWrapper(val taskManager: TaskManager, private val selfRoleDao: SelfRoleDao) {

    //guildId -> <selfRoleGroupName -> <emoteji -> <roleIds>>>
    val selfRoleCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Map<String, Map<String, List<Long>>>> { key ->
            getMap(key)
        })

    fun getMap(guildId: Long): CompletableFuture<Map<String, Map<String, List<Long>>>> {
        val future = CompletableFuture<Map<String, Map<String, List<Long>>>>()
        taskManager.async {
            val map = selfRoleDao.getMap(guildId)
            future.complete(map)
        }
        return future
    }

    suspend fun add(guildId: Long, groupName: String, emoteji: String, roleId: Long) {
        val map = selfRoleCache.get(guildId)
            .await()
            .toMutableMap()

        val pairs = map.getOrDefault(groupName, emptyMap())
            .toMutableMap()

        if (pairs[emoteji]?.contains(roleId) == false)
            pairs[emoteji] = pairs.getOrDefault(emoteji, emptyList()) + roleId

        map[groupName] = pairs
        selfRoleDao.add(guildId, groupName, emoteji, roleId)
        selfRoleCache.put(guildId, CompletableFuture.completedFuture(map))
    }

    suspend fun remove(guildId: Long, groupName: String, emoteji: String, roleId: Long) {
        // map
        val map = selfRoleCache.get(guildId)
            .await()
            .toMutableMap()

        // layer1
        val pairs = map.getOrDefault(groupName, emptyMap())
            .toMutableMap()

        // inner list
        val list = pairs.getOrDefault(emoteji, emptyList()).toMutableList()
        list.remove(roleId)

        // inserting list into layer1
        if (list.isNotEmpty()) {
            pairs[emoteji] = list
        } else {
            pairs.remove(emoteji)
        }

        // putting pairs into map
        map[groupName] = pairs

        selfRoleDao.remove(guildId, groupName, emoteji, roleId)
        selfRoleCache.put(guildId, CompletableFuture.completedFuture(map))
    }

    suspend fun remove(guildId: Long, groupName: String, emoteji: String) {
        val map = selfRoleCache.get(guildId)
            .await()
            .toMutableMap()

        val pairs = map.getOrDefault(groupName, emptyMap())
            .toMutableMap()

        pairs.remove(emoteji)
        map[groupName] = pairs

        selfRoleDao.clear(guildId, groupName, emoteji)
        selfRoleCache.put(guildId, CompletableFuture.completedFuture(map))
    }
}