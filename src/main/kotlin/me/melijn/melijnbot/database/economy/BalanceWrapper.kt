package me.melijn.melijnbot.database.economy

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.FREQUENTLY_USED_CACHE
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class BalanceWrapper(private val balanceDao: BalanceDao) {

    val balanceCache = CacheBuilder.newBuilder()
        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, Long> { key ->
            getBalance(key)
        })

    private fun getBalance(userId: Long): CompletableFuture<Long> {
        val balance = CompletableFuture<Long>()
        TaskManager.async {
            balance.complete(balanceDao.getBalance(userId))
        }
        return balance
    }

    suspend fun setBalance(userId: Long, money: Long) {
        balanceDao.setBalance(userId, money)
        balanceCache.put(userId, CompletableFuture.completedFuture(money))
    }

    suspend fun getTop(users: Int, offset: Int): Map<Long, Long> {
        return balanceDao.getTop(users, offset)
    }

    suspend fun getRowCount(): Long {
        return balanceDao.getRowCount()
    }
}