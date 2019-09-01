package me.melijn.melijnbot.database.cooldown

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class CommandCooldownWrapper(val taskManager: TaskManager, private val commandCooldownDao: CommandCooldownDao) {

    val commandCooldownCache = Caffeine.newBuilder()
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.executorService)
            .buildAsync<Long, Map<Int, Long>>() { key, executor -> getMap(key, executor) }

    private fun getMap(guildId: Long, executor: Executor = taskManager.executorService): CompletableFuture<Map<Int, Long>> {
        val future = CompletableFuture<Map<Int, Long>>()
        executor.execute {
            commandCooldownDao.getCooldowns(guildId) {
                future.complete(it)
            }
        }
        return future
    }

    fun setCooldowns(guildId: Long, commands: Set<AbstractCommand>, cooldown: Long) {
        val cooldownMap = commandCooldownCache.get(guildId).get().toMutableMap()
        for (cmd in commands) {
            if (cooldown < 1) cooldownMap.remove(cmd.id)
            else cooldownMap[cmd.id] = cooldown
        }
        if (cooldown < 1) {
            commandCooldownDao.bulkDelete(guildId, commands)
        } else {
            commandCooldownDao.bulkPut(guildId, commands, cooldown)
        }
        commandCooldownCache.put(guildId, CompletableFuture.completedFuture(cooldownMap.toMap()))
    }
}