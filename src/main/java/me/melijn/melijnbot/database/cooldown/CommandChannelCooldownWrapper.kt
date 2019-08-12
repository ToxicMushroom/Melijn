package me.melijn.melijnbot.database.cooldown

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class CommandChannelCooldownWrapper(val taskManager: TaskManager, val commandChannelCooldownDao: CommandChannelCooldownDao) {

    //channelId/guildId, userId, commandId, execTime
    val executions: MutableMap<Pair<Long, Long>, Map<Int, Long>> = HashMap()

    //chanelId
    val commandChannelCooldownCache = Caffeine.newBuilder()
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.getExecutorService())
            .buildAsync<Long, Map<Int, Long>>() { key, executor -> getCommandChannelCooldowns(key, executor) }

    fun getCommandChannelCooldowns(channelId: Long, executor: Executor = taskManager.getExecutorService()): CompletableFuture<Map<Int, Long>> {
        val map = CompletableFuture<Map<Int, Long>>()
        executor.execute {
            commandChannelCooldownDao.getCooldownMapForChannel(channelId) {
                map.complete(it)
            }
        }
        return map
    }

    fun setCooldowns(guildId: Long, channelId: Long, commands: Set<AbstractCommand>, cooldown: Long) {
        val cooldownMap = commandChannelCooldownCache.get(channelId).get().toMutableMap()
        for (cmd in commands) {
            if (cooldown < 1) cooldownMap.remove(cmd.id)
            else cooldownMap[cmd.id] = cooldown
        }
        if (cooldown < 1) {
            commandChannelCooldownDao.bulkDelete(channelId, commands)
        } else {
            commandChannelCooldownDao.bulkPut(guildId, channelId, commands, cooldown)
        }
        commandChannelCooldownCache.put(channelId, CompletableFuture.completedFuture(cooldownMap.toMap()))
    }

}