package me.melijn.melijnbot.database.cooldown

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class CommandChannelCooldownWrapper(private val commandChannelCooldownDao: CommandChannelCooldownDao) {

    //channelId/guildId, userId, commandId, execTime
    val executions: MutableMap<Pair<Long, Long>, Map<String, Long>> = HashMap()

    //chanelId
    val commandChannelCooldownCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Map<String, Long>> { key ->
            getCommandChannelCooldowns(key)
        })

    private fun getCommandChannelCooldowns(channelId: Long): CompletableFuture<Map<String, Long>> {
        val map = CompletableFuture<Map<String, Long>>()
       TaskManager.async {
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