package me.melijn.melijnbot.database.disabled

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.ChannelCommandState
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class ChannelCommandStateWrapper(val taskManager: TaskManager, private val channelCommandStateDao: ChannelCommandStateDao) {

    val channelCommandsStateCache = Caffeine.newBuilder()
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.executorService)
            .buildAsync<Long, Map<Int, ChannelCommandState>>() { key, executor -> getCommandStateMap(key, executor) }

    private fun getCommandStateMap(channelId: Long, executor: Executor = taskManager.executorService): CompletableFuture<Map<Int, ChannelCommandState>> {
        val future = CompletableFuture<Map<Int, ChannelCommandState>>()
        executor.execute {
            channelCommandStateDao.get(channelId) {
                future.complete(it)
            }
        }
        return future
    }

    fun setCommandState(guildId: Long, channelId: Long, commands: Set<AbstractCommand>, channelCommandState: ChannelCommandState) {
        val map = channelCommandsStateCache.get(channelId).get().toMutableMap()
        if (channelCommandState == ChannelCommandState.DEFAULT) {
            channelCommandStateDao.bulkRemove(channelId, commands)
            for (cmd in commands) {
                map.remove(cmd.id)
            }
        } else {
            channelCommandStateDao.bulkPut(guildId, channelId, commands, channelCommandState)
            for (cmd in commands) {
                map[cmd.id] = channelCommandState
            }
        }
        channelCommandsStateCache.put(channelId, CompletableFuture.completedFuture(map.toMap()))
    }
}