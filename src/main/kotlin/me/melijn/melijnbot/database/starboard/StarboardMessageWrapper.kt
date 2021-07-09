package me.melijn.melijnbot.database.starboard

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class StarboardMessageWrapper(private val starboardMessageDao: StarboardMessageDao) {

    suspend fun getStarboardInfo(messageId: Long): StarboardInfo? {
        val result = starboardMessageDao.getCacheEntry(messageId, HIGHER_CACHE)?.let {
            objectMapper.readValue(it, StarboardInfo::class.java)
        }
        if (result != null) return result

        val starboardInfo = starboardMessageDao.getStarboardInfo(messageId)
        if (starboardInfo != null)
            starboardMessageDao.setCacheEntry(messageId, objectMapper.writeValueAsString(starboardInfo), NORMAL_CACHE)
        return starboardInfo
    }

    fun setStarboardInfo(guildId: Long, channelId: Long, authorId: Long, messageId: Long, starboardMessageId: Long, stars: Int, deleted: Boolean, moment: Long) {
        starboardMessageDao.set(guildId, channelId, authorId, messageId, starboardMessageId, stars, deleted, moment)
        starboardMessageDao.setCacheEntry(messageId, objectMapper.writeValueAsString(
            StarboardInfo(authorId, channelId, messageId, starboardMessageId, stars, deleted, moment)
        ), NORMAL_CACHE)
    }

    fun updateDeleted(messageId: Long, deleted: Boolean) {
        starboardMessageDao.removeCacheEntry(messageId)
        starboardMessageDao.updateDeleted(messageId, deleted)
    }

    fun delete(messageId: Long) {
        starboardMessageDao.removeCacheEntry(messageId)
        starboardMessageDao.delete(messageId)
    }
}