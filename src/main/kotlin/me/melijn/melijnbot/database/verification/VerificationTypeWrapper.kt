package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.VerificationType

class VerificationTypeWrapper(private val verificationTypeDao: VerificationTypeDao) {

    suspend fun getType(guildId: Long): VerificationType {
        val cached = verificationTypeDao.getCacheEntry(guildId, HIGHER_CACHE)
        if (cached == null) {
            val type = verificationTypeDao.get(guildId)
            verificationTypeDao.setCacheEntry(guildId, type, NORMAL_CACHE)
            return type
        }
        return VerificationType.valueOf(cached)
    }

    fun setType(guildId: Long, type: VerificationType) {
        verificationTypeDao.setCacheEntry(guildId, type, NORMAL_CACHE)
        verificationTypeDao.set(guildId, type)
    }

    fun removeType(guildId: Long) {
        verificationTypeDao.setCacheEntry(guildId, VerificationType.NONE, NORMAL_CACHE)
        verificationTypeDao.set(guildId, VerificationType.NONE)
    }
}