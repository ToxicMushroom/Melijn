package me.melijn.melijnbot.database.alias

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.internals.utils.addIfNotPresent
import me.melijn.melijnbot.objectMapper

class AliasWrapper(private val aliasDao: AliasDao) {

    suspend fun getAliases(id: Long): Map<String, List<String>> {
        val cached = aliasDao.getCacheEntry(id, HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<String, List<String>>>(it)
        }
        if (cached != null) return cached

        val result = aliasDao.getAliases(id)
        aliasDao.setCacheEntry(id, result, NORMAL_CACHE)
        return result
    }

    suspend fun add(id: Long, command: String, alias: String) {
        val map = getAliases(id).toMutableMap()
        val newList = ((map[command]?.toMutableList() ?: mutableListOf()))
        val added = newList.addIfNotPresent(alias, true)
        if (added) {

            map[command] = newList
            aliasDao.insert(id, command, newList.joinToString("%SPLIT%"))
            aliasDao.setCacheEntry(id, map, NORMAL_CACHE)
        }
    }

    suspend fun remove(id: Long, command: String, alias: String) {
        val map = getAliases(id).toMutableMap()
        val newList = ((map[command]?.toMutableList() ?: mutableListOf()))

        val removed = newList.removeIf { it.equals(alias, true) }
        if (removed) {

            map[command] = newList

            if (newList.isEmpty()) {
                map.remove(command)
                aliasDao.remove(id, command)
            } else {
                aliasDao.insert(id, command, newList.joinToString("%SPLIT%"))
            }

            aliasDao.setCacheEntry(id, map, NORMAL_CACHE)
        }
    }

    fun clear(id: Long, command: String) {
        aliasDao.clear(id, command)
    }
}