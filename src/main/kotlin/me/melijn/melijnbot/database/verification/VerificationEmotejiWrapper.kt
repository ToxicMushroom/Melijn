package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class VerificationEmotejiWrapper(private val verificationEmotejiDao: VerificationEmotejiDao) {


    suspend fun getEmoteji(guildId: Long): String {
        val cached = verificationEmotejiDao.getCacheEntry(guildId, HIGHER_CACHE)
        if (cached != null) return cached

        val emoteji = verificationEmotejiDao.get(guildId)
        verificationEmotejiDao.setCacheEntry(guildId, emoteji, NORMAL_CACHE)
        return emoteji
    }

    fun setEmoteji(guildId: Long, emoteji: String) {
        verificationEmotejiDao.setCacheEntry(guildId, emoteji, NORMAL_CACHE)
        verificationEmotejiDao.set(guildId, emoteji)
    }

    fun removeEmoteji(guildId: Long) {
        verificationEmotejiDao.setCacheEntry(guildId, "", NORMAL_CACHE)
        verificationEmotejiDao.remove(guildId)
    }

}