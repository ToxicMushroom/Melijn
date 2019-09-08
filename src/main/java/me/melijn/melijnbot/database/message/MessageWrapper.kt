package me.melijn.melijnbot.database.message

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.threading.TaskManager
import net.dv8tion.jda.api.AccountType
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class MessageWrapper(val taskManager: TaskManager, private val messageDao: MessageDao) {

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
                future.complete(ModularMessage.fromJSON(json))
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


    fun removeMessage(guildId: Long, type: MessageType) {
        messageDao.remove(guildId, type)
        messageCache.put(Pair(guildId, type), CompletableFuture.completedFuture(null))
    }

    fun setMessage(guildId: Long, type: MessageType, message: ModularMessage) {
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

    fun setEmbedTitleContent(message: ModularMessage, guildId: Long, type: MessageType, arg: String) {
        val eb = EmbedBuilder(message.embed)
        eb.setTitle(arg, message.embed?.url)
        message.embed = eb.build()
        setMessage(guildId, type, message)
    }


    fun removeEmbedTitleContent(message: ModularMessage, guildId: Long, type: MessageType) {
        val eb = EmbedBuilder(message.embed)
        eb.setTitle(null)
        message.embed = validateEmbedOrNull(eb.build())
        updateMessage(message, guildId, type)
    }


    fun setEmbedTitleURL(message: ModularMessage, guildId: Long, type: MessageType, arg: String) {
        val eb = EmbedBuilder(message.embed)
        eb.setTitle(message.embed?.title, arg)
        message.embed = eb.build()
        setMessage(guildId, type, message)
    }


    fun removeEmbedTitleURL(message: ModularMessage, guildId: Long, type: MessageType) {
        val eb = EmbedBuilder(message.embed)
        eb.setTitle(null)
        message.embed = validateEmbedOrNull(eb.build())
        updateMessage(message, guildId, type)
    }


    fun setEmbedThumbnail(message: ModularMessage, guildId: Long, type: MessageType, arg: String) {
        val eb = EmbedBuilder(message.embed)
        eb.setThumbnail(arg)
        message.embed = eb.build()
        setMessage(guildId, type, message)
    }

    fun removeEmbedThumbnail(message: ModularMessage, guildId: Long, type: MessageType) {
        val eb = EmbedBuilder(message.embed)
        eb.setThumbnail(null)
        message.embed = validateEmbedOrNull(eb.build())
        updateMessage(message, guildId, type)
    }


    fun setEmbedImage(message: ModularMessage, guildId: Long, type: MessageType, arg: String) {
        val eb = EmbedBuilder(message.embed)
        eb.setImage(arg)
        message.embed = eb.build()
        setMessage(guildId, type, message)
    }

    fun removeEmbedImage(message: ModularMessage, guildId: Long, type: MessageType) {
        val eb = EmbedBuilder(message.embed)
        eb.setImage(null)
        message.embed = validateEmbedOrNull(eb.build())
        updateMessage(message, guildId, type)
    }


    fun setEmbedFooterContent(message: ModularMessage, guildId: Long, type: MessageType, arg: String) {
        val eb = EmbedBuilder(message.embed)
        eb.setFooter(arg, message.embed?.footer?.iconUrl)
        message.embed = eb.build()
        setMessage(guildId, type, message)
    }

    fun removeEmbedFooterContent(message: ModularMessage, guildId: Long, type: MessageType) {
        val eb = EmbedBuilder(message.embed)
        eb.setFooter(null)
        message.embed = validateEmbedOrNull(eb.build())
        updateMessage(message, guildId, type)
    }


    fun setEmbedFooterURL(message: ModularMessage, guildId: Long, type: MessageType, arg: String) {
        val eb = EmbedBuilder(message.embed)
        eb.setFooter(message.embed?.footer?.text, arg)
        message.embed = eb.build()
        setMessage(guildId, type, message)
    }

    fun removeEmbedFooterURL(message: ModularMessage, guildId: Long, type: MessageType) {
        val eb = EmbedBuilder(message.embed)
        eb.setFooter(message.embed?.footer?.text, null)
        message.embed = validateEmbedOrNull(eb.build())
        updateMessage(message, guildId, type)
    }


    fun setEmbedAuthorContent(message: ModularMessage, guildId: Long, type: MessageType, arg: String) {
        val eb = EmbedBuilder(message.embed)
        eb.setAuthor(arg, message.embed?.author?.url, message.embed?.author?.iconUrl)
        message.embed = eb.build()
        setMessage(guildId, type, message)
    }

    fun removeEmbedAuthorContent(message: ModularMessage, guildId: Long, type: MessageType) {
        val eb = EmbedBuilder(message.embed)
        eb.setAuthor(null)
        message.embed = validateEmbedOrNull(eb.build())
        updateMessage(message, guildId, type)
    }


    fun setEmbedAuthorURL(message: ModularMessage, guildId: Long, type: MessageType, arg: String) {
        val eb = EmbedBuilder(message.embed)
        eb.setAuthor(message.embed?.author?.name, arg, message.embed?.author?.iconUrl)
        message.embed = eb.build()
        setMessage(guildId, type, message)
    }

    fun removeEmbedAuthorURL(message: ModularMessage, guildId: Long, type: MessageType) {
        val eb = EmbedBuilder(message.embed)
        eb.setAuthor(message.embed?.author?.name, null, message.embed?.author?.iconUrl)
        message.embed = validateEmbedOrNull(eb.build())
        updateMessage(message, guildId, type)
    }

    fun setEmbedAuthorIconURL(message: ModularMessage, guildId: Long, type: MessageType, arg: String) {
        val eb = EmbedBuilder(message.embed)
        eb.setAuthor(message.embed?.author?.name, message.embed?.author?.url, arg)
        message.embed = eb.build()
        setMessage(guildId, type, message)
    }

    fun removeEmbedAuthorIconURL(message: ModularMessage, guildId: Long, type: MessageType) {
        val eb = EmbedBuilder(message.embed)
        eb.setAuthor(message.embed?.author?.name, message.embed?.author?.url, null)
        message.embed = validateEmbedOrNull(eb.build())
        updateMessage(message, guildId, type)
    }

    fun setEmbedTimestamp(message: ModularMessage, guildId: Long, type: MessageType, state: Boolean) {
        val eb = EmbedBuilder(message.embed)
        val temporalAccessor = if (state) Instant.ofEpochMilli(1) else null
        //I update the timestamp before sending depending on value

        eb.setTimestamp(temporalAccessor)
        message.embed = validateEmbedOrNull(eb.build())
        updateMessage(message, guildId, type)
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


    fun setEmbedColor(modularMessage: ModularMessage, guildId: Long, type: MessageType, color: Color) {
        val eb = EmbedBuilder(modularMessage.embed)
        eb.setColor(color)
        modularMessage.embed = eb.build()
        setMessage(guildId, type, modularMessage)
    }

    fun removeEmbedColor(modularMessage: ModularMessage, guildId: Long, type: MessageType) {
        val eb = EmbedBuilder(modularMessage.embed)
        eb.setColor(null)
        modularMessage.embed = validateEmbedOrNull(eb.build())
        updateMessage(modularMessage, guildId, type)
    }
}