package me.melijn.melijnbot.database.autopunishment

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.internals.utils.splitIETEL
import me.melijn.melijnbot.objectMapper

typealias Points = Int
typealias ExpireTime = Long

// keep track of who did what and when
class AutoPunishmentWrapper(private val autoPunishmentDao: AutoPunishmentDao) {

    // PPMap ->
    suspend fun getPointsMap(guildId: Long, userId: Long): Map<ExpireTime, Map<String, Points>> {
        val cached: Map<ExpireTime, String>? = autoPunishmentDao.getCacheEntry("$guildId:$userId", HIGHER_CACHE)?.let {
            objectMapper.readValue(it)
        }
        val final = if (cached == null) {
            val result = autoPunishmentDao.get(guildId, userId)
            autoPunishmentDao.setCacheEntry(
                "$guildId:$userId",
                objectMapper.writeValueAsString(result),
                NORMAL_CACHE
            )
            result
        } else {
            cached
        }

        // expire -> List<punishGroupAndPointsStrings>
        val pointsMap = mutableMapOf<ExpireTime, List<String>>()
        for ((expire, map) in final) {
            if (expire == 0L || expire > System.currentTimeMillis()) {
                pointsMap[expire] = (pointsMap[expire] ?: emptyList()) +
                    map.removePrefix("[")
                        .removeSuffix("]")
                        .splitIETEL("],[")
            }
        }

        // expire -> Map<PunishGroupName, Points>
        val chonkMap = mutableMapOf<ExpireTime, Map<String, Points>>()
        for ((expire, punishGroupAndPointsStrings) in pointsMap) {
            val map = mutableMapOf<String, Points>()
            for (entry in punishGroupAndPointsStrings) {
                val ePair = entry.split(", ")
                map[ePair[0]] = ePair[1].toInt() // PunishGroupName -> Points
            }
            chonkMap[expire] = map
        }

        return chonkMap
    }

    fun set(guildId: Long, userId: Long, pointsMap: Map<ExpireTime, Map<String, Points>>) {
        val sqlMap = mutableMapOf<ExpireTime, String>()
        pointsMap.forEach { entry ->
            val string = entry.value
                .map { pgPointsEntry -> "${pgPointsEntry.key}, ${pgPointsEntry.value}" }
                .joinToString("],[", "[", "]")
            sqlMap[entry.key] = string
        }

        autoPunishmentDao.bulkSet(guildId, userId, sqlMap)
        autoPunishmentDao.removeCacheEntry("$guildId:$userId")
    }

    fun removeExpiredEntries() {
        autoPunishmentDao.removeEntriesOlderThen(System.currentTimeMillis())
    }
}