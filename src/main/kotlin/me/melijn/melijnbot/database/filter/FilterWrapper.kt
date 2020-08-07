package me.melijn.melijnbot.database.filter

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.FilterType
import me.melijn.melijnbot.internals.utils.addIfNotPresent
import me.melijn.melijnbot.objectMapper

class FilterWrapper(private val filterDao: FilterDao) {

    //guildId, filterName, filter

    suspend fun getFilters(guildId: Long, filterGroupName: String, filterType: FilterType): List<String> {
        val cached = filterDao.getCacheEntry("$filterType:$guildId:$filterGroupName", HIGHER_CACHE)?.let {
            objectMapper.readValue<List<String>>(it)
        }
        if (cached != null) return cached

        val filters = filterDao.get(guildId, filterGroupName, filterType)
        filterDao.setCacheEntry("$filterType:$guildId:$filterGroupName", objectMapper.writeValueAsString(filters), NORMAL_CACHE)
        return filters
    }

    suspend fun addFilter(guildId: Long, filterGroupName: String, filterType: FilterType, filter: String) {
        filterDao.add(guildId, filterGroupName, filterType, filter)

        val filters = getFilters(guildId, filterGroupName, filterType).toMutableList()
        filters.addIfNotPresent(filter)
        filterDao.setCacheEntry("$filterType:$guildId:$filterGroupName", objectMapper.writeValueAsString(filters), NORMAL_CACHE)
    }

    suspend fun removeFilter(guildId: Long, filterGroupName: String, filterType: FilterType, filter: String) {
        filterDao.remove(guildId, filterGroupName, filterType, filter)

        val filters = getFilters(guildId, filterGroupName, filterType).toMutableList()
        filters.remove(filter)
        filterDao.setCacheEntry("$filterType:$guildId:$filterGroupName", objectMapper.writeValueAsString(filters), NORMAL_CACHE)
    }

    suspend fun contains(guildId: Long, filterGroupName: String, filterType: FilterType, filter: String): Boolean {
        return getFilters(guildId, filterGroupName, filterType).contains(filter)
    }
}