package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class BotLogStateWrapper(
    private val botLogStateDao: BotLogStateDao
) {

    suspend fun shouldLog(guildId: Long): Boolean {
        val result = botLogStateDao.getCacheEntry("$guildId", HIGHER_CACHE)?.toBoolean()

        if (result != null) return result

        val state = botLogStateDao.contains(guildId)
        botLogStateDao.setCacheEntry(guildId, state, NORMAL_CACHE)
        return state
    }

    fun set(guildId: Long, state: Boolean) {
        if (state) {
            botLogStateDao.add(guildId)
        } else {
            botLogStateDao.delete(guildId)
        }
        botLogStateDao.setCacheEntry(guildId, state, NORMAL_CACHE)
    }
}