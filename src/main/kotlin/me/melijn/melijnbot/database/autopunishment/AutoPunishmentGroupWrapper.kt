package me.melijn.melijnbot.database.autopunishment

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.enums.PointsTriggerType
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class AutoPunishmentGroupWrapper(val taskManager: TaskManager, private val autoPunishmentGroupDao: AutoPunishmentGroupDao) {

    val autoPunishmentCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, List<PunishGroup>> { pair ->
            getPunishGroups(pair)
        })

    private fun getPunishGroups(guildId: Long): CompletableFuture<List<PunishGroup>> {
        val future = CompletableFuture<List<PunishGroup>>()

        taskManager.async {
            val valuePairs = autoPunishmentGroupDao.getAll(guildId)
            val list = mutableListOf<PunishGroup>()
            for ((group, valuePair) in valuePairs) {
                val firstEntries = valuePair
                    .first
                    .removeSurrounding("[", "]")
                    .split("],[")
                val secondEntries = valuePair
                    .second
                    .removeSurrounding("[", "]")
                    .split("],[")
                val ppTriggerMap = mutableMapOf<PointsTriggerType, Int>()
                val ppGoalMap = mutableMapOf<Int, String>()
                for (entry in firstEntries) {
                    val entryParts = entry.split(", ")
                    ppTriggerMap[PointsTriggerType.valueOf(entryParts[0])] = entryParts[1].toInt()
                }
                for (entry in secondEntries) {
                    val entryParts = entry.split(", ")
                    ppGoalMap[entryParts[0].toInt()] = entryParts[1]
                }
                list.add(PunishGroup(group, ppTriggerMap, ppGoalMap))
            }
            future.complete(list)
        }

        return future
    }

    suspend fun getMapsForGuild(guildId: Long): Map<String, Pair<Map<PointsTriggerType, Int>, Map<Int, String>>> {
        val maps = autoPunishmentGroupDao.getAll(guildId)
        val final = mutableMapOf<String, Pair<Map<PointsTriggerType, Int>, Map<Int, String>>>()
        for ((group, pair) in maps) {
            val firstEntries = pair
                .first
                .removeSurrounding("[", "]")
                .split("],[")
            val secondEntries = pair
                .second
                .removeSurrounding("[", "]")
                .split("],[")
            val ppTriggerMap = mutableMapOf<PointsTriggerType, Int>()
            val ppGoalMap = mutableMapOf<Int, String>()
            for (entry in firstEntries) {
                val entryParts = entry.split(", ")
                ppTriggerMap[PointsTriggerType.valueOf(entryParts[0])] = entryParts[1].toInt()
            }
            for (entry in secondEntries) {
                val entryParts = entry.split(", ")
                ppGoalMap[entryParts[0].toInt()] = entryParts[1]
            }
            final[group] = Pair(ppTriggerMap, ppGoalMap)
        }
        return final
    }

    suspend fun add(guildId: Long, group: String) {
        autoPunishmentGroupDao.add(guildId, group)
        autoPunishmentCache.invalidate(Pair(guildId, group))
    }

    suspend fun setTypePointsMap(guildId: Long, group: String, types: Map<PointsTriggerType, Int>) {
        val string = types
            .map { entry -> "${entry.key}, ${entry.value}" }
            .joinToString("],[", "[", "]")
        autoPunishmentGroupDao.setTypePointsMap(guildId, group, string)
        autoPunishmentCache.invalidate(Pair(guildId, group))
    }

    suspend fun setPointGoalMap(guildId: Long, group: String, goals: Map<Int, String>) {
        val string = goals
            .map { entry -> "${entry.key}, ${entry.value}" }
            .joinToString("],[", "[", "]")
        autoPunishmentGroupDao.setTypePointsMap(guildId, group, string)
        autoPunishmentCache.invalidate(Pair(guildId, group))
    }

    suspend fun remove(guildId: Long, group: String) {
        autoPunishmentGroupDao.remove(guildId, group)
        autoPunishmentCache.invalidate(Pair(guildId, group))
    }
}

data class PunishGroup(
    val groupName: String,
    val typePointsMap: Map<PointsTriggerType, Int>,
    val pointGoalMap: MutableMap<Int, String>
)