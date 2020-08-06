package me.melijn.melijnbot.database.time

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import java.util.*

class TimeZoneWrapper(private val timeZoneDao: TimeZoneDao) {

    suspend fun getTimeZone(guildId: Long): String {
        val cached = timeZoneDao.getCacheEntry(guildId, HIGHER_CACHE)
        if (cached == null) {
            val timezone = timeZoneDao.getZoneId(guildId)
            timeZoneDao.setCacheEntry(guildId, timezone, NORMAL_CACHE)
            return timezone
        }
        return cached
    }

    fun removeTimeZone(id: Long) {
        timeZoneDao.remove(id)
        timeZoneDao.setCacheEntry(id, "", NORMAL_CACHE)
    }

    fun setTimeZone(id: Long, zone: TimeZone) {
        timeZoneDao.put(id, zone.id)
        timeZoneDao.setCacheEntry(id, zone.id, NORMAL_CACHE)
    }
}