package me.melijn.melijnbot.database.language

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class GuildLanguageWrapper(private val languageDao: GuildLanguageDao) {

    suspend fun getLanguage(userId: Long): String {
        val result = languageDao.getCacheEntry(userId, HIGHER_CACHE)
        if (result != null) return result

        val language = languageDao.get(userId)
        languageDao.setCacheEntry(userId, language, NORMAL_CACHE)
        return language
    }

    fun setLanguage(userId: Long, language: String) {
        languageDao.set(userId, language)
        languageDao.setCacheEntry(userId, language, NORMAL_CACHE)
    }

    fun removeLanguage(authorId: Long) {
        languageDao.remove(authorId)
        languageDao.setCacheEntry(authorId, "", NORMAL_CACHE)
    }
}