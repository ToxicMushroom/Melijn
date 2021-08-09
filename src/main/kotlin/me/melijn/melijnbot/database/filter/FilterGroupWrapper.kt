package me.melijn.melijnbot.database.filter

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class FilterGroupWrapper(private val filterGroupDao: FilterGroupDao) {

    suspend fun getGroups(guildId: Long): List<FilterGroup> {
        val cached = filterGroupDao.getCacheEntry(guildId, HIGHER_CACHE)?.let {
            objectMapper.readValue<List<FilterGroup>>(it)
        }
        if (cached != null) return cached

        val list = filterGroupDao.get(guildId)
        filterGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
        return list
    }

    suspend fun putGroup(guildId: Long, group: FilterGroup) {
        val list = getGroups(guildId).toMutableList()
        list.removeIf { (groupId) -> groupId == group.filterGroupName }
        list.add(group)
        filterGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
        filterGroupDao.add(guildId, group)
    }

    suspend fun deleteGroup(guildId: Long, group: FilterGroup) {
        val list = getGroups(guildId).toMutableList()
        list.removeIf { (groupId) -> groupId == group.filterGroupName }
        filterGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
        filterGroupDao.remove(guildId, group)
    }
}