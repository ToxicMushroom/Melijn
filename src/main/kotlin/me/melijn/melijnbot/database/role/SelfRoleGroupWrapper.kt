package me.melijn.melijnbot.database.role

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class SelfRoleGroupWrapper(private val selfRoleGroupDao: SelfRoleGroupDao) {

    suspend fun getMap(guildId: Long): List<SelfRoleGroup> {
        val result = selfRoleGroupDao.getCacheEntry(guildId, HIGHER_CACHE)
            ?.let { objectMapper.readValue<List<SelfRoleGroup>>(it) }

        if (result != null) return result

        val selfroleGroups = selfRoleGroupDao.get(guildId)
        selfRoleGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(selfroleGroups), NORMAL_CACHE)
        return selfroleGroups
    }

    suspend fun insertOrUpdate(guildId: Long, selfRoleGroup: SelfRoleGroup) {
        val list = getMap(guildId).toMutableList()
        list.firstOrNull { (groupName) ->
            groupName == selfRoleGroup.groupName
        }?.let { group ->
            list.remove(group)
        }

        list.add(selfRoleGroup)

        selfRoleGroupDao.set(
            guildId,
            selfRoleGroup.groupName,
            selfRoleGroup.messageIds.joinToString("%SPLIT%"),
            selfRoleGroup.channelId,
            selfRoleGroup.isEnabled,
            selfRoleGroup.pattern
                ?: "",
            selfRoleGroup.isSelfRoleable,
            selfRoleGroup.limitToOneRole
        )
        selfRoleGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }

    suspend fun delete(guildId: Long, groupName1: String) {
        val list = getMap(guildId).toMutableList()
        list.firstOrNull { (groupName) ->
            groupName == groupName1
        }?.let { group ->
            list.remove(group)
        }

        selfRoleGroupDao.remove(guildId, groupName1)
        selfRoleGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }
}