package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.internals.models.ModularMessage

class MessageWrapper(private val messageDao: MessageDao) {

    suspend fun getMessage(guildId: Long, msgName: String): ModularMessage? {
        val result = messageDao.getCacheEntry("$msgName:$guildId", HIGHER_CACHE)?.let {
            try {
                if (it.isBlank()) return null
                ModularMessage.fromJSON(it)
            } catch (t: Throwable) {
                t.printStackTrace()
                return null
            }
        }

        if (result != null) return result

        val json = messageDao.get(guildId, msgName)
        if (json == null) {
            messageDao.setCacheEntry("$msgName:$guildId", "", NORMAL_CACHE)
            return null
        }

        val modular = try {
            ModularMessage.fromJSON(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        messageDao.setCacheEntry("$msgName:$guildId", modular?.toJSON() ?: "", NORMAL_CACHE)

        return modular
    }

    fun updateMessage(message: ModularMessage, guildId: Long, msgName: String) {
        if (message.isEmpty()) {
            removeMessage(guildId, msgName)
        } else {
            (setMessage(guildId, msgName, message))
        }
    }

    fun removeMessage(guildId: Long, msgName: String) {
        messageDao.remove(guildId, msgName)
        messageDao.setCacheEntry("$msgName:$guildId", "", NORMAL_CACHE)
    }

    fun setMessage(guildId: Long, msgName: String, message: ModularMessage) {
        messageDao.set(guildId, msgName, message.toJSON())
        messageDao.setCacheEntry("$msgName:$guildId", message.toJSON(), NORMAL_CACHE)
    }

    fun shouldRemove(message: ModularMessage): Boolean {
        val content = message.messageContent
        val embed = message.embed
        val attachments = message.attachments

        return content == null &&
            attachments.isEmpty() &&
            (embed == null || embed.isEmpty || !embed.isSendable)
    }

    suspend fun getMessages(guildId: Long): List<String> {
        return messageDao.getMessages(guildId)
    }
}