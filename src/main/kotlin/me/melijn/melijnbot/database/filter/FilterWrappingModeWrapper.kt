package me.melijn.melijnbot.database.filter

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.enums.WrappingMode
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class FilterWrappingModeWrapper(val taskManager: TaskManager, private val filterWrappingModeDao: FilterWrappingModeDao) {

    val filterWrappingModeCache = Caffeine.newBuilder()
        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Pair<Long, Long?>, WrappingMode> { key, executor -> getMode(key.first, key.second, executor) }

    private fun getMode(guildId: Long, channelId: Long?, executor: Executor): CompletableFuture<WrappingMode> {
        val future = CompletableFuture<WrappingMode>()
        executor.launch {
            val mode = filterWrappingModeDao.get(guildId, channelId)
            future.complete(mode)
        }
        return future
    }

    suspend fun setMode(guildId: Long, channelId: Long?, mode: WrappingMode) {
        filterWrappingModeCache.put(Pair(guildId, channelId), CompletableFuture.completedFuture(mode))
        filterWrappingModeDao.set(guildId, channelId, mode)
    }

    suspend fun unsetMode(guildId: Long, channelId: Long?) {
        filterWrappingModeCache.put(Pair(guildId, channelId), CompletableFuture.completedFuture(WrappingMode.DEFAULT))
        filterWrappingModeDao.unset(guildId, channelId)
    }

}