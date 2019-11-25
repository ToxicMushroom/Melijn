package me.melijn.melijnbot.database.autopunishment

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class AutoPunishmentWrapper(val taskManager: TaskManager, private val autoPunishmentDao: AutoPunishmentDao) {

    val autoPunishmentCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Pair<Long, Long>, String> { pair ->
            getPointsMap(pair)
        })

    private fun getPointsMap(pair: Pair<Long, Long>): CompletableFuture<String> {
        val future = CompletableFuture<String>()

        taskManager.async {
            val pointsMap = autoPunishmentDao.get(pair.first, pair.second)
            future.complete(pointsMap)
        }

        return future
    }

    suspend fun set(guildId: Long, userId: Long, pointsMap: Map<Long, Long>) {
        val string = pointsMap
            .map { entry -> "${entry.key}, ${entry.value}" }
            .joinToString("],[", "[", "]")
        autoPunishmentDao.set(guildId, userId, string)
        autoPunishmentCache.put(Pair(guildId, userId), CompletableFuture.completedFuture(string))
    }
}