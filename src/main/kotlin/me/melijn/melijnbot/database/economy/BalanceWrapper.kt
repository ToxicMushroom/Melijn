package me.melijn.melijnbot.database.economy

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class BalanceWrapper(private val balanceDao: BalanceDao) {

    suspend fun getBalance(userId: Long): Long {
        val cached = balanceDao.getCacheEntry(userId, HIGHER_CACHE)?.toLong()
        if (cached != null) return cached

        val balance = balanceDao.getBalance(userId)
        balanceDao.setCacheEntry(userId, balance, NORMAL_CACHE)
        return balance
    }

    fun setBalance(userId: Long, money: Long) {
        balanceDao.setBalance(userId, money)
        balanceDao.setCacheEntry(userId, money, NORMAL_CACHE)
    }

    suspend fun getTop(users: Int, offset: Int): Map<Long, Long> {
        return balanceDao.getTop(users, offset)
    }

    suspend fun getRowCount(): Long {
        return balanceDao.getRowCount()
    }
}