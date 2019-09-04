package me.melijn.melijnbot.database.message

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.threading.TaskManager
import net.dv8tion.jda.api.JDA
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class MessageWrapper (val taskManager: TaskManager, private val jda: JDA, private val messageDao: MessageDao) {

    val messageCache = Caffeine.newBuilder()
            .executor(taskManager.executorService)
            .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
            .buildAsync<Pair<Long, MessageType>, ModularMessage?> { key, _ ->
                getMessage(key)
            }

    private fun getMessage(pair: Pair<Long, MessageType>): CompletableFuture<ModularMessage?> {
        val future = CompletableFuture<ModularMessage?>()
        CoroutineScope(taskManager.dispatcher).launch {
            val json = messageDao.get(pair.first, pair.second)
            if (json == null) {
                future.complete(null)
                return@launch
            }
            try {
                future.complete(ModularMessage.fromJSON(jda, json))
            } catch (e: Exception) {
                e.printStackTrace()
                future.complete(null)
            }

        }
        return future
    }

    suspend fun setMessageContent(guildId: Long, messageType: MessageType, content: String?) {
        val message = messageCache.get(Pair(guildId, messageType)).await() ?: ModularMessage()
        message.messageContent = content
        messageDao.set(guildId, messageType, message.toJSON())
        messageCache.put(Pair(guildId, messageType), CompletableFuture.completedFuture(message))
    }

    fun removeMessage(guildId: Long, type: MessageType) {
        messageDao.remove(guildId, type)
        messageCache.put(Pair(guildId, type), CompletableFuture.completedFuture(null))
    }
}