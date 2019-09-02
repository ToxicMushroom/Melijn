package me.melijn.melijnbot.database.message

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class MessageWrapper (val taskManager: TaskManager, private val messageDao: MessageDao) {

    val messageCache = Caffeine.newBuilder()
            .executor(taskManager.executorService)
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .buildAsync<Pair<Long, MessageType>, String> { key, _ ->
                getMessage(key)
            }

    private fun getMessage(pair: Pair<Long, MessageType>): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        CoroutineScope(taskManager.dispatcher).launch {
            future.complete(messageDao.get(pair.first, pair.second))
        }
        return future
    }

    fun setMessage(guildId: Long, messageType: MessageType, content: String) {
        messageDao.set(guildId, messageType, content)
        messageCache.put(Pair(guildId, messageType), CompletableFuture.completedFuture(content))
    }

    fun removeMessage(guildId: Long, type: MessageType) {
        messageDao.remove(guildId, type)
        messageCache.put(Pair(guildId, type), CompletableFuture.completedFuture(""))
    }
}