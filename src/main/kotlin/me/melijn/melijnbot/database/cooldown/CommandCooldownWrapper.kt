package me.melijn.melijnbot.database.cooldown

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper
import java.util.concurrent.CompletableFuture

class CommandCooldownWrapper(private val commandCooldownDao: CommandCooldownDao) {

    suspend fun getMap(guildId: Long): Map<String, Long> {
        val cached = commandCooldownDao.getCacheEntry(guildId, HIGHER_CACHE)?.let { objectMapper.readValue<Map<String, Long>>(it) }
        if (cached != null) return cached

        val result = commandCooldownDao.getCooldowns(guildId)
        commandCooldownDao.setCacheEntry(guildId, objectMapper.writeValueAsString(result), NORMAL_CACHE)
        return result
    }

    suspend fun setCooldowns(guildId: Long, commands: Set<String>, cooldown: Long) {
        val cooldownMap = getMap(guildId).toMutableMap()
        for (id in commands) {
            if (cooldown < 1) {
                cooldownMap.remove(id)
            } else {
                cooldownMap[id] = cooldown
            }
        }
        if (cooldown < 1) {
            commandCooldownDao.bulkDelete(guildId, commands)
        } else {
            commandCooldownDao.bulkPut(guildId, commands, cooldown)
        }
        commandCooldownDao.setCacheEntry(guildId, CompletableFuture.completedFuture(cooldownMap.toMap()), NORMAL_CACHE)
    }
}