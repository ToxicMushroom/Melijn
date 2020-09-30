package me.melijn.melijnbot.database.autopunishment

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class PunishmentWrapper(private val punishmentDao: PunishmentDao) {

    suspend fun getList(guildId: Long): List<Punishment> {
        val cached = punishmentDao.getCacheEntry(guildId, HIGHER_CACHE)?.let {
            objectMapper.readValue<List<Punishment>>(it)
        }
        if (cached != null) return cached

        val result = punishmentDao.get(guildId)
        punishmentDao.setCacheEntry(guildId, objectMapper.writeValueAsString(result), NORMAL_CACHE)
        return result
    }

    suspend fun put(guildId: Long, punishment: Punishment) {
        punishmentDao.put(guildId, punishment)
        val list = getList(guildId).toMutableList()
        list.removeIf { (pName) ->
            pName == punishment.name
        }
        list.add(punishment)
        punishmentDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }

    suspend fun remove(guildId: Long, name: String) {
        punishmentDao.remove(guildId, name)
        val list = getList(guildId).toMutableList()
        list.removeIf { name == it.name }
        punishmentDao.setCacheEntry(guildId, objectMapper.writeValueAsString(
            list
        ), NORMAL_CACHE)
    }
}