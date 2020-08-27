package me.melijn.melijnbot.database.time

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import java.util.*

class TimeZoneWrapper(private val timeZoneDao: TimeZoneDao) {

    suspend fun getTimeZone(id: Long): String {
        val cached = timeZoneDao.getCacheEntry(id, HIGHER_CACHE)

        if (cached != null) return cached

        val timezone = timeZoneDao.getZoneId(id)
        timeZoneDao.setCacheEntry(id, timezone, NORMAL_CACHE)
        return timezone
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