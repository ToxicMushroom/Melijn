package me.melijn.melijnbot.database.cooldown

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class CommandCooldownWrapper(val taskManager: TaskManager, private val commandCooldownDao: CommandCooldownDao) {

    val commandCooldownCache = Caffeine.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Long, Map<String, Long>>() { key, executor -> getMap(key, executor) }

    private fun getMap(guildId: Long, executor: Executor = taskManager.executorService): CompletableFuture<Map<String, Long>> {
        val future = CompletableFuture<Map<String, Long>>()
        executor.launch {
            val map = commandCooldownDao.getCooldowns(guildId)
            future.complete(map)
        }
        return future
    }

    suspend fun setCooldowns(guildId: Long, commands: Set<String>, cooldown: Long) {
        val cooldownMap = commandCooldownCache.get(guildId).await().toMutableMap()
        for (id in commands) {
            if (cooldown < 1) {
                cooldownMap.remove(id)
            } else {
                cooldownMap[id] = cooldown
            }
        }
        if (cooldown < 1) {
            commandCooldownDao.bulkDelete(guildId, commands)
        } else {
            commandCooldownDao.bulkPut(guildId, commands, cooldown)
        }
        commandCooldownCache.put(guildId, CompletableFuture.completedFuture(cooldownMap.toMap()))
    }
}