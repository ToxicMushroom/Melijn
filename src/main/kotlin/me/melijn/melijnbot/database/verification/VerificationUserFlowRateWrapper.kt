package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class VerificationUserFlowRateWrapper(private val verificationUserFlowRateDao: VerificationUserFlowRateDao) {


    suspend fun getFlowRate(guildId: Long): Long {
        val cached = verificationUserFlowRateDao.getCacheEntry(guildId, HIGHER_CACHE)
        if (cached == null) {
            val flowRate = verificationUserFlowRateDao.get(guildId)
            verificationUserFlowRateDao.setCacheEntry(guildId, flowRate, NORMAL_CACHE)
            return flowRate
        }
        return cached.toLong()
    }

    fun setUserFlowRate(guildId: Long, rate: Long) {
        verificationUserFlowRateDao.setCacheEntry(guildId, rate, NORMAL_CACHE)
        verificationUserFlowRateDao.set(guildId, rate)
    }

    fun removeFlowRate(guildId: Long) {
        verificationUserFlowRateDao.setCacheEntry(guildId, -1, NORMAL_CACHE)
        verificationUserFlowRateDao.remove(guildId)
    }
}