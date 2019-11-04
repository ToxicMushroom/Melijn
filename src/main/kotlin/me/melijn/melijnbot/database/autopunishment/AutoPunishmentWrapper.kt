package me.melijn.melijnbot.database.autopunishment

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class AutoPunishmentWrapper(val taskManager: TaskManager, private val autoPunishmentDao: AutoPunishmentDao) {

    val autoPunishmentCache = Caffeine.newBuilder()
        .executor(taskManager.executorService)
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .buildAsync<Pair<Long, Long>, String> { pair, executor -> getPointsMap(pair, executor) }

    private fun getPointsMap(pair: Pair<Long, Long>, executor: Executor): CompletableFuture<String> {
        val future = CompletableFuture<String>()

        CoroutineScope(executor.asCoroutineDispatcher()).launch {
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