package me.melijn.melijnbot.database.autopunishment

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.PointsTriggerType
import me.melijn.melijnbot.internals.utils.splitIETEL

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
                .first
                .splitIETEL(",")
            val secondEntries = valuePair
                .second
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
            list.add(PunishGroup(group, ppTriggerList, ppGoalMap))
        }
        punishmentGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
        return list
    }

    suspend fun getMapsForGuild(guildId: Long): Map<String, Pair<List<PointsTriggerType>, Map<Int, String>>> {
        val maps = punishmentGroupDao.getAll(guildId)
        val final = mutableMapOf<String, Pair<List<PointsTriggerType>, Map<Int, String>>>()
        for ((group, pair) in maps) {
            val firstEntries = if (pair.first.isEmpty()) emptyList() else pair
                .first
                .splitIETEL(",")

            val secondEntries = if (pair.first.isEmpty()) emptyList() else pair
                .second
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
        list.add(PunishGroup(group, emptyList(), mutableMapOf()))
        punishmentGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }


    suspend fun setTriggerTypes(guildId: Long, group: String, types: List<PointsTriggerType>) {
        val string = types.joinToString(",") { type ->
            "$type"
        }

        punishmentGroupDao.setEnabledTypes(guildId, group, string)
        val list = getList(guildId).toMutableList()
        val pGroup = list.first { it.groupName == group }
        list.remove(pGroup)
        list.add(PunishGroup(pGroup.groupName, types, pGroup.pointGoalMap))
        punishmentGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }

    suspend fun setPointGoalMap(guildId: Long, group: String, goals: Map<Int, String>) {
        val string = goals
            .map { entry -> "${entry.key}, ${entry.value}" }
            .joinToString("],[", "[", "]")

        punishmentGroupDao.setPointGoalMap(guildId, group, string)
        val list = getList(guildId).toMutableList()
        val pGroup = list.first { it.groupName == group }
        list.remove(pGroup)
        list.add(PunishGroup(pGroup.groupName, pGroup.enabledTypes, goals.toMutableMap()))
        punishmentGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }

    suspend fun remove(guildId: Long, group: String) {
        punishmentGroupDao.remove(guildId, group)
        val list = getList(guildId).toMutableList()
        val pGroup = list.first { it.groupName == group }
        list.remove(pGroup)
        punishmentGroupDao.setCacheEntry(guildId, objectMapper.writeValueAsString(list), NORMAL_CACHE)
    }
}

data class PunishGroup(
    val groupName: String,
    var enabledTypes: List<PointsTriggerType>,
    val pointGoalMap: MutableMap<Int, String>
)