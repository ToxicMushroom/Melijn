package me.melijn.melijnbot.database.role

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper
import net.dv8tion.jda.api.utils.data.DataArray

class SelfRoleWrapper(private val selfRoleDao: SelfRoleDao) {

    // guildId -> <selfRoleGroupName -> emotejiInfo (see SelfRoleDao for example)

    suspend fun getMap(guildId: Long): Map<String, DataArray> {
        val result = selfRoleDao.getCacheEntry(guildId, HIGHER_CACHE)?.let { map ->
            objectMapper.readValue<Map<String, String>>(map).mapValues {
                DataArray.fromJson(it.value)
            }
        }

        if (result != null) return result

        val map = selfRoleDao.getMap(guildId)
        selfRoleDao.setCacheEntry(
            guildId,
            objectMapper.writeValueAsString(map.mapValues { it.value.toString() }),
            NORMAL_CACHE
        )
        return map
    }

    suspend fun set(guildId: Long, groupName: String, emoteji: String, roleId: Long, chance: Int = 100) {
        val map = getMap(guildId)
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
        selfRoleDao.setCacheEntry(
            guildId,
            objectMapper.writeValueAsString(map.mapValues { it.value.toString() }), NORMAL_CACHE
        )
    }

    suspend fun remove(guildId: Long, groupName: String, emoteji: String, roleId: Long) {
        // map
        val map = getMap(guildId)
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
        selfRoleDao.setCacheEntry(
            guildId,
            objectMapper.writeValueAsString(map.mapValues { it.value.toString() }),
            NORMAL_CACHE
        )
    }

    suspend fun remove(guildId: Long, groupName: String, emoteji: String) {
        // map
        val map = getMap(guildId)
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
        selfRoleDao.setCacheEntry(
            guildId,
            objectMapper.writeValueAsString(map.mapValues { it.value.toString() }),
            NORMAL_CACHE
        )
    }

    suspend fun update(guildId: Long, groupName: String, data: DataArray) {
        // map
        val map = getMap(guildId)
            .toMutableMap()

        map[groupName] = data

        selfRoleDao.set(guildId, groupName, data.toString())
        selfRoleDao.setCacheEntry(
            guildId,
            objectMapper.writeValueAsString(map.mapValues { it.value.toString() }),
            NORMAL_CACHE
        )
    }

    suspend fun changeName(guildId: Long, name1: String, name2: String) {
        val map = getMap(guildId).toMutableMap()
        val data = map[name1]
        data?.let {
            map.remove(name1)
            map[name2] = it
        }
        selfRoleDao.changeName(guildId, name1, name2)
        selfRoleDao.setCacheEntry(
            guildId,
            objectMapper.writeValueAsString(map.mapValues { it.value.toString() }),
            NORMAL_CACHE
        )

    }

    suspend fun clear(guildId: Long, groupName: String): Int {
        return selfRoleDao.clear(guildId, groupName)
    }
}