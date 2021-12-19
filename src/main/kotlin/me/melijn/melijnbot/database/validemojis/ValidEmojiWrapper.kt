package me.melijn.melijnbot.database.validemojis

class ValidEmojiWrapper(private val validEmojiDao: ValidEmojiDao) {

    fun set(emoji: String) {
        validEmojiDao.set(emoji)
        validEmojiDao.setCacheEntry(emoji, true, 1)
    }

    suspend fun isValid(emoji: String): Boolean {
        var validCache = validEmojiDao.getBooleanFromCache(emoji, 5)
        if (validCache == null) {
            validCache = validEmojiDao.isValid(emoji)
            validEmojiDao.setCacheEntry(emoji, validCache, 1)
            return validCache
        }
        return validCache
    }
}