package me.melijn.melijnbot.database.command

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class CustomCommandWrapper(private val customCommandDao: CustomCommandDao) {

    suspend fun getList(guildId: Long): List<CustomCommand> {
        val cached = customCommandDao.getCacheEntry(guildId, HIGHER_CACHE)?.let {
            objectMapper.readValue<List<CustomCommand>>(it)
        }
        if (cached != null) return cached

        val result = customCommandDao.getForGuild(guildId)
        customCommandDao.setCacheEntry(guildId, objectMapper.writeValueAsString(result), NORMAL_CACHE)
        return result
    }

    suspend fun add(guildId: Long, cc: CustomCommand): Long {
        val id = customCommandDao.add(guildId, cc)
        val list = getList(guildId).toMutableList()
        cc.id = id
        list.add(cc)
        customCommandDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
        return id
    }

    suspend fun remove(guildId: Long, id: Long) {
        val list = getList(guildId).toMutableList()
        list.removeIf { it.id == id }
        customCommandDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
        customCommandDao.remove(guildId, id)
    }

    suspend fun update(guildId: Long, cc: CustomCommand) {
        val list = getList(guildId).toMutableList()
        list.removeIf { it.id == cc.id }
        list.add(cc)
        customCommandDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
        customCommandDao.update(guildId, cc)
    }

    suspend fun getCCById(guildId: Long, id: Long?): CustomCommand? {
        if (id == null) return null
        val list = getList(guildId)
        return list.firstOrNull { (id1) -> id1 == id }
    }
}