package me.melijn.melijnbot.database.message

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import net.dv8tion.jda.api.AccountType
import net.dv8tion.jda.api.entities.MessageEmbed
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class MessageWrapper(val taskManager: TaskManager, private val messageDao: MessageDao) {

    val messageCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Pair<Long, MessageType>, ModularMessage?> { key ->
            getMessage(key)
        })

    private fun getMessage(pair: Pair<Long, MessageType>): CompletableFuture<ModularMessage?> {
        val future = CompletableFuture<ModularMessage?>()
        CoroutineScope(taskManager.dispatcher).launch {
            val json = messageDao.get(pair.first, pair.second)
            if (json == null) {
                future.complete(null)
                return@launch
            }

            try {
                future.complete(ModularMessage.fromJSON(json))
            } catch (e: Exception) {
                e.printStackTrace()
                future.complete(null)
            }
        }
        return future
    }

    suspend fun updateMessage(message: ModularMessage, guildId: Long, type: MessageType) {
        if (shouldRemove(message)) {
            removeMessage(guildId, type)
        } else {
            (setMessage(guildId, type, message))
        }
    }


    suspend fun removeMessage(guildId: Long, type: MessageType) {
        messageDao.remove(guildId, type)
        messageCache.put(Pair(guildId, type), CompletableFuture.completedFuture(null))
    }

    suspend fun setMessage(guildId: Long, type: MessageType, message: ModularMessage) {
        messageDao.set(guildId, type, message.toJSON())
        messageCache.put(Pair(guildId, type), CompletableFuture.completedFuture(message))
    }

    fun shouldRemove(message: ModularMessage): Boolean {
        val content = message.messageContent
        val embed = message.embed
        val attachments = message.attachments

        return content == null &&
            attachments.isEmpty() &&
            (embed == null || embed.isEmpty || !embed.isSendable(AccountType.BOT))
    }

    private fun validateEmbedOrNull(embed: MessageEmbed): MessageEmbed? =
        if (embed.isEmpty || !embed.isSendable(AccountType.BOT)) {
            null
        } else {
            embed
        }
}