package me.melijn.melijnbot.database.role

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import net.dv8tion.jda.api.utils.data.DataArray
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class SelfRoleWrapper(val taskManager: TaskManager, private val selfRoleDao: SelfRoleDao) {

    // guildId -> <selfRoleGroupName -> emotejiInfo (see SelfRoleDao for example)
    val selfRoleCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Map<String, DataArray>> { key ->
            getMap(key)
        })

    fun getMap(guildId: Long): CompletableFuture<Map<String, DataArray>> {
        val future = CompletableFuture<Map<String, DataArray>>()
        taskManager.async {
            val map = selfRoleDao.getMap(guildId)
            future.complete(map)
        }
        return future
    }

    suspend fun set(guildId: Long, groupName: String, emoteji: String, roleId: Long, chance: Int = 100) {
        val map = selfRoleCache.get(guildId)
            .await()
            .toMutableMap()

        val data = map.getOrDefault(groupName, DataArray.empty())
        var containsEmoteji = false

        for (i in 0 until data.length()) {
            val entryData = data.getArray(i)
            if (entryData.getString(0) == emoteji) {
                containsEmoteji = true
                val rolesArr = entryData.getArray(2)

                for (j in 0 until rolesArr.length()) {
                    val roleInfoArr = rolesArr.getArray(j)
                    if (roleInfoArr.getLong(1) == roleId) {
                        rolesArr.remove(j)
                        break
                    }
                }

                rolesArr.add(
                    DataArray.empty()
                        .add(chance)
                        .add(roleId)
                )

                entryData.remove(2)
                val boolValue = entryData.getBoolean(2)
                entryData.remove(2)

                entryData.add(rolesArr)
                entryData.add(boolValue)

                data.remove(i)
                data.add(entryData)
                break
            }
        }

        if (!containsEmoteji) {
            data.add(
                DataArray.empty()
                    .add(emoteji)
                    .add("")
                    .add(
                        DataArray.empty().add(
                            DataArray.empty()
                                .add(chance)
                                .add(roleId)
                        )
                    ).add(true)
            )
        }

        map[groupName] = data
        selfRoleDao.set(guildId, groupName, data.toString())
        selfRoleCache.put(guildId, CompletableFuture.completedFuture(map))
    }

    suspend fun remove(guildId: Long, groupName: String, emoteji: String, roleId: Long) {
        // map
        val map = selfRoleCache.get(guildId)
            .await()
            .toMutableMap()

        // layer1
        val data = map.getOrDefault(groupName, DataArray.empty())

        // inner list
        for (i in 0 until data.length()) {
            val entryData = data.getArray(i)
            if (entryData.getString(0) == emoteji) {
                val rolesArr = entryData.getArray(2)

                for (j in 0 until rolesArr.length()) {
                    val roleInfoArr = rolesArr.getArray(j)
                    if (roleInfoArr.getLong(1) == roleId) {
                        rolesArr.remove(j)
                        break
                    }
                }

                // reconstruct array
                entryData.remove(2)
                val boolValue = entryData.getBoolean(2)
                entryData.remove(2)

                entryData.add(rolesArr)
                entryData.add(boolValue)

                data.remove(i)
                data.add(entryData)
                break
            }
        }

        // putting pairs into map
        map[groupName] = data

        selfRoleDao.set(guildId, groupName, data.toString())
        selfRoleCache.put(guildId, CompletableFuture.completedFuture(map))
    }

    suspend fun remove(guildId: Long, groupName: String, emoteji: String) {
        // map
        val map = selfRoleCache.get(guildId)
            .await()
            .toMutableMap()

        // layer1
        val data = map.getOrDefault(groupName, DataArray.empty())

        // inner list
        for (i in 0 until data.length()) {
            val entryData = data.getArray(i)
            if (entryData.getString(0) == emoteji) {
                data.remove(i)
                break
            }
        }

        // putting pairs into map
        map[groupName] = data

        selfRoleDao.set(guildId, groupName, data.toString())
        selfRoleCache.put(guildId, CompletableFuture.completedFuture(map))
    }

    suspend fun update(guildId: Long, groupName: String, data: DataArray) {
        // map
        val map = selfRoleCache.get(guildId)
            .await()
            .toMutableMap()

        map[groupName] = data

        selfRoleDao.set(guildId, groupName, data.toString())
        selfRoleCache.put(guildId, CompletableFuture.completedFuture(map))
    }
}