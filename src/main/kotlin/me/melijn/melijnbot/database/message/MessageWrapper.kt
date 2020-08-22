package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.MessageType
import net.dv8tion.jda.api.entities.MessageEmbed

class MessageWrapper(private val messageDao: MessageDao) {

    suspend fun getMessage(guildId: Long, msgType: MessageType): ModularMessage? {
        val result = messageDao.getCacheEntry("$msgType:$guildId", HIGHER_CACHE)?.let {
            try {
                if (it.isBlank()) return null
                ModularMessage.fromJSON(it)
            } catch (t: Throwable) {
                t.printStackTrace()
                return null
            }
        }

        if (result != null) return result

        val json = messageDao.get(guildId, msgType)
        if (json == null) {
            messageDao.setCacheEntry("$msgType:$guildId", "", NORMAL_CACHE)
            return null
        }

        val modular = try {
            ModularMessage.fromJSON(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        messageDao.setCacheEntry("$msgType:$guildId", modular?.toJSON() ?: "", NORMAL_CACHE)

        return modular
    }

    fun updateMessage(message: ModularMessage, guildId: Long, type: MessageType) {
        if (shouldRemove(message)) {
            removeMessage(guildId, type)
        } else {
            (setMessage(guildId, type, message))
        }
    }


    fun removeMessage(guildId: Long, type: MessageType) {
        messageDao.remove(guildId, type)
        messageDao.setCacheEntry("$type:$guildId", "", NORMAL_CACHE)
    }

    fun setMessage(guildId: Long, type: MessageType, message: ModularMessage) {
        messageDao.set(guildId, type, message.toJSON())
        messageDao.setCacheEntry("$type:$guildId", message.toJSON(), NORMAL_CACHE)
    }

    fun shouldRemove(message: ModularMessage): Boolean {
        val content = message.messageContent
        val embed = message.embed
        val attachments = message.attachments

        return content == null &&
            attachments.isEmpty() &&
            (embed == null || embed.isEmpty || !embed.isSendable)
    }

    private fun validateEmbedOrNull(embed: MessageEmbed): MessageEmbed? =
        if (embed.isEmpty || !embed.isSendable) {
            null
        } else {
            embed
        }
}