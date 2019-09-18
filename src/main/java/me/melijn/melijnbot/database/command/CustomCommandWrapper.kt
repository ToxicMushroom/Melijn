package me.melijn.melijnbot.database.command

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class CustomCommandWrapper(private val taskManager: TaskManager, private val customCommandDao: CustomCommandDao) {

    val customCommandCache = Caffeine.newBuilder()
        .executor(taskManager.executorService)
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.HOURS)
        .buildAsync<Long, Map<Long, CustomCommand>> { key: Long, executor: Executor -> getCustomCommands(key, executor)}

    private fun getCustomCommands(guildId: Long, executor: Executor): CompletableFuture<Map<Long, CustomCommand>> {
        val future = CompletableFuture<Map<Long, CustomCommand>>()

        CoroutineScope(executor.asCoroutineDispatcher()).launch {
            val customCommands = customCommandDao.getForGuild(guildId)
            future.complete(customCommands)
        }

        return future
    }

    suspend fun add(guildId: Long, cc: CustomCommand) {
        val id = customCommandDao.add(guildId, cc)
       // val map = customCommandCache.(guildId).await()
    }

    fun remove(id: Long) {

    }
}