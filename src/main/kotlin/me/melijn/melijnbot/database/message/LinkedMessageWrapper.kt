package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.MessageType
import net.dv8tion.jda.api.entities.MessageEmbed

class LinkedMessageWrapper(private val linkedMessageDao: LinkedMessageDao) {

    suspend fun getMessage(guildId: Long, msgType: MessageType): String? {
        val cached = linkedMessageDao.getCacheEntry("$msgType:$guildId", HIGHER_CACHE)
        if (cached != null) {
            if (cached.isEmpty()) return null
            return cached
        }

        val result = linkedMessageDao.get(guildId, msgType)
        if (result == null) {
            linkedMessageDao.setCacheEntry("$msgType:$guildId", "", NORMAL_CACHE)
            return null
        }

        linkedMessageDao.setCacheEntry("$msgType:$guildId", result, NORMAL_CACHE)
        return result
    }

    fun removeMessage(guildId: Long, type: MessageType) {
        linkedMessageDao.remove(guildId, type)
        linkedMessageDao.setCacheEntry("$type:$guildId", "", NORMAL_CACHE)
    }

    fun setMessage(guildId: Long, type: MessageType, msgName: String) {
        linkedMessageDao.set(guildId, type, msgName)
        linkedMessageDao.setCacheEntry("$type:$guildId", msgName, NORMAL_CACHE)
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