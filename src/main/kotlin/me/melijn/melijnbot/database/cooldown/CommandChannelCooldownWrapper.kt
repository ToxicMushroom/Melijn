package me.melijn.melijnbot.database.cooldown

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class CommandChannelCooldownWrapper(private val commandChannelCooldownDao: CommandChannelCooldownDao) {

    //channelId/guildId, userId, commandId, execTime
    val executions: MutableMap<Pair<Long, Long>, Map<String, Long>> = HashMap()

    suspend fun getMap(channelId: Long): Map<String, Long> {
        val cached = commandChannelCooldownDao.getCacheEntry(channelId, HIGHER_CACHE)?.let {
            objectMapper.readValue<Map<String, Long>>(it)
        }
        if (cached != null) return cached

        val result = commandChannelCooldownDao.getCooldownMapForChannel(channelId)
        commandChannelCooldownDao.setCacheEntry(channelId, objectMapper.writeValueAsString(result), NORMAL_CACHE)
        return result
    }

    suspend fun setCooldowns(guildId: Long, channelId: Long, commandIds: Set<String>, cooldown: Long) {
        val cooldownMap = getMap(channelId).toMutableMap()
        for (cmdId in commandIds) {
            if (cooldown < 1) cooldownMap.remove(cmdId)
            else cooldownMap[cmdId] = cooldown
        }
        if (cooldown < 1) {
            commandChannelCooldownDao.bulkDelete(channelId, commandIds)
        } else {
            commandChannelCooldownDao.bulkPut(guildId, channelId, commandIds, cooldown)
        }
        commandChannelCooldownDao.setCacheEntry(channelId, objectMapper.writeValueAsString(cooldownMap), NORMAL_CACHE)
    }

    fun migrateChannel(oldId: Long, newId: Long) {
        commandChannelCooldownDao.migrateChannel(oldId, newId)
    }

    fun invalidate(channelId: Long) {
        commandChannelCooldownDao.removeCacheEntry("$channelId")
    }

}