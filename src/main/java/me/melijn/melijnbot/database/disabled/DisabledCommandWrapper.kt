package me.melijn.melijnbot.database.disabled

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.CommandState
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class DisabledCommandWrapper(val taskManager: TaskManager, private val disabledCommandDao: DisabledCommandDao) {

    //guildId | commandId (or ccId)
    val disabledCommandsCache = Caffeine.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .executor(taskManager.executorService)
        .buildAsync<Long, Set<String>>() { key, executor -> getDisabledCommandSet(key, executor) }

    private fun getDisabledCommandSet(guildId: Long, executor: Executor = taskManager.executorService): CompletableFuture<Set<String>> {
        val future = CompletableFuture<Set<String>>()
        executor.launch {
            val id = disabledCommandDao.get(guildId)
            future.complete(id)
        }
        return future
    }

    suspend fun setCommandState(guildId: Long, commandIds: Set<String>, commandState: CommandState) {
        val set = disabledCommandsCache.get(guildId).await().toMutableSet()

        if (commandState == CommandState.DISABLED) {
            for (id in commandIds) {
                if (!set.contains(id))
                    set.add(id)
            }
            disabledCommandDao.bulkPut(guildId, commandIds)
        } else {
            for (id in commandIds) {
                set.remove(id)
            }
            disabledCommandDao.bulkDelete(guildId, commandIds)
        }
        disabledCommandsCache.put(guildId, CompletableFuture.completedFuture(set.toSet()))
    }
}