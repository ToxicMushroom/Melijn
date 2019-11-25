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
        .build(loadingCacheFrom<Pair<Long, Long>, Map<PointsTriggerType, Int>> { pair ->
            getPointsMap(pair)
        })

    private fun getPointsMap(pair: Pair<Long, Long>): CompletableFuture<Map<PointsTriggerType, Int>> {
        val future = CompletableFuture<Map<PointsTriggerType, Int>>()

        taskManager.async {
            val pointsMap = autoPunishmentGroupDao.get(pair.first, pair.second)
            val entries = pointsMap
                .removeSurrounding("[", "]")
                .split("],[")
            val map = mutableMapOf<PointsTriggerType, Int>()
            for (entry in entries) {
                val entryParts = entry.split(", ")
                map[PointsTriggerType.valueOf(entryParts[0])] = entryParts[1].toInt()
            }
            future.complete(map)
        }

        return future
    }

    suspend fun set(guildId: Long, groupId: Long, types: Map<PointsTriggerType, Int>) {
        val string = types
            .map { entry -> "${entry.key}, ${entry.value}" }
            .joinToString("],[", "[", "]")
        autoPunishmentGroupDao.set(guildId, groupId, string)
        autoPunishmentCache.put(Pair(guildId, groupId), CompletableFuture.completedFuture(types))
    }
}