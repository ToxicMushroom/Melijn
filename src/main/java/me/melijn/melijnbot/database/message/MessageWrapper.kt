package me.melijn.melijnbot.database.message

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.threading.TaskManager
import net.dv8tion.jda.api.AccountType
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class MessageWrapper(val taskManager: TaskManager, private val jda: JDA, private val messageDao: MessageDao) {

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

    private fun updateMessage(message: ModularMessage, guildId: Long, type: MessageType) {
        if (shouldRemove(message)) removeMessage(guildId, type)
        else (setMessage(guildId, type, message))
    }


    private fun removeMessage(guildId: Long, type: MessageType) {
        messageDao.remove(guildId, type)
        messageCache.put(Pair(guildId, type), CompletableFuture.completedFuture(null))
    }

    private fun setMessage(guildId: Long, type: MessageType, message: ModularMessage) {
        messageDao.set(guildId, type, message.toJSON())
        messageCache.put(Pair(guildId, type), CompletableFuture.completedFuture(message))
    }

    private fun shouldRemove(message: ModularMessage): Boolean {
        val content = message.messageContent
        val embed = message.embed
        val attachments = message.attachments

        return content == null &&
                attachments.isEmpty() &&
                (embed == null || embed.isEmpty || !embed.isSendable(AccountType.BOT))
    }

    fun setEmbedDescription(message: ModularMessage, guildId: Long, type: MessageType, arg: String) {
        val eb = EmbedBuilder(message.embed)
        eb.setDescription(arg)
        message.embed = eb.build()
        setMessage(guildId, type, message)
    }


    fun removeEmbedDescription(message: ModularMessage, guildId: Long, type: MessageType) {
        val eb = EmbedBuilder(message.embed)
        eb.setDescription(null)
        message.embed = validateEmbedOrNull(eb.build())
        updateMessage(message, guildId, type)
    }

    private fun validateEmbedOrNull(embed: MessageEmbed): MessageEmbed? =
            if (embed.isEmpty || !embed.isSendable(AccountType.BOT)) {
                null
            } else {
                embed
            }


    fun setMessageContent(message: ModularMessage, guildId: Long, type: MessageType, content: String?) {
        message.messageContent = content
        setMessage(guildId, type, message)
    }


    fun removeMessageContent(message: ModularMessage, guildId: Long, type: MessageType) {
        message.messageContent = null
        updateMessage(message, guildId, type)
    }


    fun clearEmbed(message: ModularMessage, guildId: Long, type: MessageType) {
        message.embed = null
        updateMessage(message, guildId, type)
    }
}