package me.melijn.melijnbot.database.autopunishment

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.PointsTriggerType
import me.melijn.melijnbot.internals.utils.splitIETEL
import me.melijn.melijnbot.objectMapper

class PunishmentGroupWrapper(private val punishmentGroupDao: PunishmentGroupDao) {

    suspend fun getList(guildId: Long): List<PunishGroup> {
        val cached = punishmentGroupDao.getCacheEntry(guildId, HIGHER_CACHE)?.let {
            objectMapper.readValue<List<PunishGroup>>(it)
        }

        if (cached != null) return cached

        val valuePairs = punishmentGroupDao.getAll(guildId)
        val list = mutableListOf<PunishGroup>()
        for ((group, valuePair) in valuePairs) {
            val firstEntries = valuePair
                .second
                .splitIETEL(",")
            val secondEntries = valuePair
                .third
                .removeSurrounding("[", "]")
                .splitIETEL("],[")
            val ppTriggerList = mutableListOf<PointsTriggerType>()
            val ppGoalMap = mutableMapOf<Int, String>()
            for (entry in firstEntries) {
                ppTriggerList.add(PointsTriggerType.valueOf(entry))
            }
            for (entry in secondEntries) {
                val entryParts = entry.split(", ")
                ppGoalMap[entryParts[0].toInt()] = entryParts[1]
            }
            list.add(PunishGroup(group, valuePair.first, ppTriggerList, ppGoalMap))
        }
        punishmentGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
        return list
    }

    suspend fun getMapsForGuild(guildId: Long): Map<String, Pair<List<PointsTriggerType>, Map<Int, String>>> {
        val maps = punishmentGroupDao.getAll(guildId)
        val final = mutableMapOf<String, Pair<List<PointsTriggerType>, Map<Int, String>>>()
        for ((group, pair) in maps) {
            val firstEntries = if (pair.second.isEmpty()) emptyList() else pair
                .second
                .splitIETEL(",")

            val secondEntries = if (pair.third.isEmpty()) emptyList() else pair
                .third
                .removeSurrounding("[", "]")
                .splitIETEL("],[")
            val ppTriggerList = mutableListOf<PointsTriggerType>()
            val ppGoalMap = mutableMapOf<Int, String>()
            for (entry in firstEntries) {
                ppTriggerList.add(PointsTriggerType.valueOf(entry))
            }
            for (entry in secondEntries) {
                val entryParts = entry.split(", ")
                ppGoalMap[entryParts[0].toInt()] = entryParts[1]
            }
            final[group] = Pair(ppTriggerList, ppGoalMap)
        }
        return final
    }

    suspend fun add(guildId: Long, group: String) {
        val list = getList(guildId).toMutableList()
        if (list.any { it.groupName == group }) return
        punishmentGroupDao.add(guildId, group)
        list.add(PunishGroup(group, 0,  emptyList(), mutableMapOf()))
        punishmentGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }

    suspend fun setTriggerTypes(guildId: Long, group: String, types: List<PointsTriggerType>) {
        val string = types.joinToString(",") { type ->
            "$type"
        }

        punishmentGroupDao.setEnabledTypes(guildId, group, string)
        val list = getList(guildId).toMutableList()
        val pGroup = list.firstOrNull { it.groupName == group } ?: return
        list.remove(pGroup)
        list.add(PunishGroup(pGroup.groupName, pGroup.expireTime, types, pGroup.pointGoalMap))
        punishmentGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }

    suspend fun setPointGoalMap(guildId: Long, group: String, goals: Map<Int, String>) {
        val string = goals
            .map { entry -> "${entry.key}, ${entry.value}" }
            .joinToString("],[", "[", "]")

        punishmentGroupDao.setPointGoalMap(guildId, group, string)
        val list = getList(guildId).toMutableList()
        val pGroup = list.firstOrNull { it.groupName == group } ?: return
        list.remove(pGroup)
        list.add(PunishGroup(pGroup.groupName, pGroup.expireTime, pGroup.enabledTypes, goals.toMutableMap()))
        punishmentGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }

    suspend fun remove(guildId: Long, group: String) {
        punishmentGroupDao.remove(guildId, group)
        val list = getList(guildId).toMutableList()
        val pGroup = list.firstOrNull { it.groupName == group } ?: return
        list.remove(pGroup)
        punishmentGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }

    suspend fun setExpireTime(guildId: Long, groupName: String, expireMillis: Long) {
        punishmentGroupDao.setExpireTime(guildId, groupName, expireMillis)
        val list = getList(guildId).toMutableList()
        val pGroup = list.firstOrNull { it.groupName == groupName } ?: return
        list.remove(pGroup)
        list.add(PunishGroup(pGroup.groupName, expireMillis, pGroup.enabledTypes, pGroup.pointGoalMap))
        punishmentGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }
}

data class PunishGroup(
    val groupName: String,
    var expireTime: ExpireTime, // millis
    var enabledTypes: List<PointsTriggerType>,
    val pointGoalMap: MutableMap<Int, String>
)