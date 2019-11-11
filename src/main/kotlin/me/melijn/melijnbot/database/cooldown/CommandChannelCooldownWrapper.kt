package me.melijn.melijnbot.database.cooldown

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class CommandChannelCooldownWrapper(val taskManager: TaskManager, private val commandChannelCooldownDao: CommandChannelCooldownDao) {

    //channelId/guildId, userId, commandId, execTime
    val executions: MutableMap<Pair<Long, Long>, Map<String, Long>> = HashMap()

    //chanelId
    val commandChannelCooldownCache = Caffeine.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Long, Map<String, Long>>() { key, executor -> getCommandChannelCooldowns(key, executor) }

    private fun getCommandChannelCooldowns(channelId: Long, executor: Executor = taskManager.executorService): CompletableFuture<Map<String, Long>> {
        val map = CompletableFuture<Map<String, Long>>()
        executor.execute {
            commandChannelCooldownDao.getCooldownMapForChannel(channelId) {
                map.complete(it)
            }
        }
        return map
    }

    suspend fun setCooldowns(guildId: Long, channelId: Long, commandIds: Set<String>, cooldown: Long) {
        val cooldownMap = commandChannelCooldownCache.get(channelId).await().toMutableMap()
        for (cmdId in commandIds) {
            if (cooldown < 1) cooldownMap.remove(cmdId)
            else cooldownMap[cmdId] = cooldown
        }
        if (cooldown < 1) {
            commandChannelCooldownDao.bulkDelete(channelId, commandIds)
        } else {
            commandChannelCooldownDao.bulkPut(guildId, channelId, commandIds, cooldown)
        }
        commandChannelCooldownCache.put(channelId, CompletableFuture.completedFuture(cooldownMap.toMap()))
    }

}