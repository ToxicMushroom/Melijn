package me.melijn.melijnbot.database.autopunishment

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.enums.PointsTriggerType
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import me.melijn.melijnbot.internals.utils.splitIETEL
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class PunishmentGroupWrapper(val taskManager: TaskManager, private val punishmentGroupDao: PunishmentGroupDao) {

    val autoPunishmentCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, List<PunishGroup>> { pair ->
            getPunishGroups(pair)
        })


    private fun getPunishGroups(guildId: Long): CompletableFuture<List<PunishGroup>> {
        val future = CompletableFuture<List<PunishGroup>>()

        taskManager.async {
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
            future.complete(list)
        }

        return future
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
        punishmentGroupDao.add(guildId, group)
        autoPunishmentCache.invalidate(Pair(guildId, group))
    }

    suspend fun setTriggerTypes(guildId: Long, group: String, types: List<PointsTriggerType>) {
        val string = types.joinToString(",") { type ->
            "$type"
        }

        punishmentGroupDao.setEnabledTypes(guildId, group, string)
        autoPunishmentCache.invalidate(Pair(guildId, group))
    }

    suspend fun setPointGoalMap(guildId: Long, group: String, goals: Map<Int, String>) {
        val string = goals
            .map { entry -> "${entry.key}, ${entry.value}" }
            .joinToString("],[", "[", "]")

        punishmentGroupDao.setPointGoalMap(guildId, group, string)
        autoPunishmentCache.invalidate(Pair(guildId, group))
    }

    suspend fun remove(guildId: Long, group: String) {
        punishmentGroupDao.remove(guildId, group)
        autoPunishmentCache.invalidate(Pair(guildId, group))
    }
}

data class PunishGroup(
    val groupName: String,
    var enabledTypes: List<PointsTriggerType>,
    val pointGoalMap: MutableMap<Int, String>
)