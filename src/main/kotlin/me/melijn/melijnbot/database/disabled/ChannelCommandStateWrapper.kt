package me.melijn.melijnbot.database.disabled

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.ChannelCommandState
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class ChannelCommandStateWrapper(val taskManager: TaskManager, private val channelCommandStateDao: ChannelCommandStateDao) {

    val channelCommandsStateCache = Caffeine.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Long, Map<String, ChannelCommandState>>() { key, executor -> getCommandStateMap(key, executor) }

    private fun getCommandStateMap(channelId: Long, executor: Executor = taskManager.executorService): CompletableFuture<Map<String, ChannelCommandState>> {
        val future = CompletableFuture<Map<String, ChannelCommandState>>()
        executor.launch {
            val id = channelCommandStateDao.get(channelId)
            future.complete(id)
        }
        return future
    }

    suspend fun setCommandState(guildId: Long, channelId: Long, commandIds: Set<String>, channelCommandState: ChannelCommandState) {
        val map = channelCommandsStateCache.get(channelId).await().toMutableMap()
        if (channelCommandState == ChannelCommandState.DEFAULT) {
            channelCommandStateDao.bulkRemove(channelId, commandIds)
            for (id in commandIds) {
                map.remove(id)
            }
        } else {
            channelCommandStateDao.bulkPut(guildId, channelId, commandIds, channelCommandState)
            for (id in commandIds) {
                map[id] = channelCommandState
            }
        }
        channelCommandsStateCache.put(channelId, CompletableFuture.completedFuture(map.toMap()))
    }
}