package me.melijn.melijnbot.database.language

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class UserLanguageWrapper(private val userLanguageDao: UserLanguageDao) {

    suspend fun getLanguage(userId: Long): String {
        val result = userLanguageDao.getCacheEntry(userId, HIGHER_CACHE)
        if (result != null) return result

        val language = userLanguageDao.get(userId)
        userLanguageDao.setCacheEntry(userId, language, NORMAL_CACHE)
        return language
    }

    fun setLanguage(userId: Long, language: String) {
        userLanguageDao.set(userId, language)
        userLanguageDao.setCacheEntry(userId, language, NORMAL_CACHE)
    }

    fun removeLanguage(authorId: Long) {
        userLanguageDao.remove(authorId)
        userLanguageDao.setCacheEntry(authorId, "", NORMAL_CACHE)
    }
}