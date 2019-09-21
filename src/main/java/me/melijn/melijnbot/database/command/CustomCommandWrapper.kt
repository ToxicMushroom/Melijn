package me.melijn.melijnbot.database.command

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class CustomCommandWrapper(private val taskManager: TaskManager, private val customCommandDao: CustomCommandDao) {

    val customCommandCache = Caffeine.newBuilder()
        .executor(taskManager.executorService)
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.HOURS)
        .buildAsync<Long, List<CustomCommand>> { key: Long, executor: Executor -> getCustomCommands(key, executor)}

    private fun getCustomCommands(guildId: Long, executor: Executor): CompletableFuture<List<CustomCommand>> {
        val future = CompletableFuture<List<CustomCommand>>()

        executor.launch {
            val customCommands = customCommandDao.getForGuild(guildId)
            future.complete(customCommands)
        }

        return future
    }

    suspend fun add(guildId: Long, cc: CustomCommand) {
        val id = customCommandDao.add(guildId, cc)
        val list = customCommandCache.get(guildId).await().toMutableList()
        cc.id = id
        list.add(cc)
        customCommandCache.put(guildId, CompletableFuture.completedFuture(list))
    }

    suspend fun remove(guildId: Long, id: Long) {
        customCommandDao.remove(guildId, id)
    }

    suspend fun update(guildId: Long, cc: CustomCommand) {
        customCommandDao.update(guildId, cc)
    }
}