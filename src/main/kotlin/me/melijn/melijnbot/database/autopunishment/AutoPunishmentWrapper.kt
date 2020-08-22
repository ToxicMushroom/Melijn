package me.melijn.melijnbot.database.autopunishment

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.internals.utils.splitIETEL

//keep track of who did what and when
class AutoPunishmentWrapper(private val autoPunishmentDao: AutoPunishmentDao) {

    suspend fun getPointsMap(guildId: Long, userId: Long): Map<String, Long> {
        val cached = autoPunishmentDao.getCacheEntry("$guildId:$userId", HIGHER_CACHE)?.let {
            it
        }
        val final = if (cached == null) {
            val result = autoPunishmentDao.get(guildId, userId)
            autoPunishmentDao.setCacheEntry("$guildId:$userId", result, NORMAL_CACHE)
            result
        } else {
            cached
        }

        val pointsMap = final
            .removePrefix("[")
            .removeSuffix("]")
            .splitIETEL("],[")

        val newMap = mutableMapOf<String, Long>()
        for (entry in pointsMap) {
            val ePair = entry.split(", ")
            newMap[ePair[0]] = ePair[1].toLong()
        }

        return newMap
    }

    fun set(guildId: Long, userId: Long, pointsMap: Map<String, Long>) {
        val string = pointsMap
            .map { entry -> "${entry.key}, ${entry.value}" }
            .joinToString("],[", "[", "]")
        autoPunishmentDao.set(guildId, userId, string)
        autoPunishmentDao.setCacheEntry("$guildId:$userId", string)
    }
}