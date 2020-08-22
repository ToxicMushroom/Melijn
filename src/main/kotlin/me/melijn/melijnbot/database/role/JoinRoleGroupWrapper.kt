package me.melijn.melijnbot.database.role

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class JoinRoleGroupWrapper(private val joinRoleGroupDao: JoinRoleGroupDao) {

    suspend fun getList(guildId: Long): List<JoinRoleGroupInfo> {
        val result = joinRoleGroupDao.getCacheEntry(guildId, HIGHER_CACHE)?.let {
            objectMapper.readValue<List<JoinRoleGroupInfo>>(it)
        }

        if (result != null) return result

        val list = joinRoleGroupDao.get(guildId)
        joinRoleGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
        return list
    }

    suspend fun insertOrUpdate(guildId: Long, selfRoleGroup: JoinRoleGroupInfo) {
        val list = getList(guildId).toMutableList()
        list.firstOrNull { (groupName) ->
            groupName == selfRoleGroup.groupName
        }?.let { group ->
            list.remove(group)
        }

        list.add(selfRoleGroup)

        joinRoleGroupDao.put(guildId, selfRoleGroup)
        joinRoleGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }

    suspend fun delete(guildId: Long, groupName1: String) {
        val list = getList(guildId).toMutableList()
        list.firstOrNull { (groupName) ->
            groupName == groupName1
        }?.let { group ->
            list.remove(group)
        }

        joinRoleGroupDao.remove(guildId)
        joinRoleGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }
}