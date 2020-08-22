package me.melijn.melijnbot.database.disabled

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.CommandState
import me.melijn.melijnbot.objectMapper

class DisabledCommandWrapper(private val disabledCommandDao: DisabledCommandDao) {

    //guildId | commandId (or ccId)

    suspend fun getSet(guildId: Long): Set<String> {
        val cached = disabledCommandDao.getCacheEntry(guildId, HIGHER_CACHE)?.let {
            objectMapper.readValue<Set<String>>(it)
        }
        if (cached != null) return cached

        val set = disabledCommandDao.get(guildId)
        disabledCommandDao.setCacheEntry(guildId, objectMapper.writeValueAsString(set), NORMAL_CACHE)
        return set
    }

    suspend fun setCommandState(guildId: Long, commandIds: Set<String>, commandState: CommandState) {
        val set = getSet(guildId).toMutableSet()

        if (commandState == CommandState.DISABLED) {
            for (id in commandIds) {
                if (!set.contains(id))
                    set.add(id)
            }
            disabledCommandDao.bulkPut(guildId, commandIds)
        } else {
            for (id in commandIds) {
                set.remove(id)
            }
            disabledCommandDao.bulkDelete(guildId, commandIds)
        }
        disabledCommandDao.setCacheEntry(guildId, objectMapper.writeValueAsString(set), NORMAL_CACHE)
    }
}