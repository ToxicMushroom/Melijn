package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class VerificationPasswordWrapper(private val verificationPasswordDao: VerificationPasswordDao) {

    suspend fun getPassword(guildId: Long): String {
        val cached = verificationPasswordDao.getCacheEntry(guildId, HIGHER_CACHE)
        if (cached == null) {
            val password = verificationPasswordDao.get(guildId)
            verificationPasswordDao.setCacheEntry(guildId, password, NORMAL_CACHE)
            return password
        }
        return cached
    }

    fun set(guildId: Long, password: String) {
        verificationPasswordDao.setCacheEntry(guildId, password, NORMAL_CACHE)
        verificationPasswordDao.set(guildId, password)
    }

    fun remove(guildId: Long) {
        verificationPasswordDao.setCacheEntry(guildId, "", NORMAL_CACHE)
        verificationPasswordDao.remove(guildId)
    }

}