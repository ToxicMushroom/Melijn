package me.melijn.melijnbot.database.disabled

import com.github.benmanes.caffeine.cache.Caffeine
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.CommandState
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class DisabledCommandWrapper(val taskManager: TaskManager, private val disabledCommandDao: DisabledCommandDao) {

    val disabledCommandsCache = Caffeine.newBuilder()
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .executor(taskManager.executorService)
            .buildAsync<Long, Set<Int>>() { key, executor -> getDisabledCommandSet(key, executor) }

    private fun getDisabledCommandSet(guildId: Long, executor: Executor = taskManager.executorService): CompletableFuture<Set<Int>> {
        val future = CompletableFuture<Set<Int>>()
        executor.execute {
            disabledCommandDao.get(guildId) {
                future.complete(it)
            }
        }
        return future
    }

    fun setCommandState(guildId: Long, commands: Set<AbstractCommand>, commandState: CommandState) {
        val set = disabledCommandsCache.get(guildId).get().toMutableSet()

        if (commandState == CommandState.DISABLED) {
            for (cmd in commands) {
                set.add(cmd.id)
            }
            disabledCommandDao.bulkPut(guildId, commands)
        } else {
            for (cmd in commands) {
                set.remove(cmd.id)
            }
            disabledCommandDao.bulkDelete(guildId, commands)
        }
        disabledCommandsCache.put(guildId, CompletableFuture.completedFuture(set.toSet()))
    }
}