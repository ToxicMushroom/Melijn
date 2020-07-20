package me.melijn.melijnbot.database.autopunishment

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import me.melijn.melijnbot.internals.utils.splitIETEL
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

//keep track of who did what and when
class AutoPunishmentWrapper(private val autoPunishmentDao: AutoPunishmentDao) {

    //guildId, userId, pointsMap
    val autoPunishmentCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Pair<Long, Long>, Map<String, Long>> { pair ->
            getPointsMap(pair)
        })

    fun getPointsMap(pair: Pair<Long, Long>): CompletableFuture<Map<String, Long>> {
        val future = CompletableFuture<Map<String, Long>>()

        TaskManager.async {
            val pointsMap = autoPunishmentDao.get(pair.first, pair.second)
                .removePrefix("[")
                .removeSuffix("]")
                .splitIETEL("],[")

            val newMap = mutableMapOf<String, Long>()
            for (entry in pointsMap) {
                val ePair = entry.split(", ")
                newMap[ePair[0]] = ePair[1].toLong()
            }

            future.complete(newMap)
        }

        return future
    }

    suspend fun set(guildId: Long, userId: Long, pointsMap: Map<String, Long>) {
        val string = pointsMap
            .map { entry -> "${entry.key}, ${entry.value}" }
            .joinToString("],[", "[", "]")
        autoPunishmentDao.set(guildId, userId, string)
        autoPunishmentCache.put(Pair(guildId, userId), CompletableFuture.completedFuture(pointsMap))
    }
}