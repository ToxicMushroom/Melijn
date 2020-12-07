package me.melijn.melijnbot.database.economy

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BalanceDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "balances"
    override val tableStructure: String = "userId bigint, money bigint"
    override val primaryKey: String = "userId"

    override val cacheName: String = "balance"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun getBalance(userId: Long): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId=?", { rs ->
            if (rs.next()) {
                it.resume(rs.getLong("money"))
            } else {
                it.resume(0)
            }
        }, userId)
    }

    fun setBalance(userId: Long, money: Long) {
        driverManager.executeUpdate(
            "INSERT INTO $table (userId, money) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET money=?",
            userId, money, money
        )
    }

    suspend fun getPosition(userId: Long): Pair<Long, Long> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM (SELECT *, row_number() OVER (ORDER BY money DESC) as position FROM $table) x WHERE userId = ?",
            { rs ->
                if (rs.next()) {
                    it.resume(Pair(rs.getLong("money"), rs.getLong("position")))
                } else {
                    it.resume(Pair(0, -1))
                }
            },
            userId
        )
    }

    suspend fun getTop(users: Int, offset: Int): Map<Long, Long> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table ORDER BY money DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
            { rs ->
                val map = mutableMapOf<Long, Long>()

                while (rs.next()) {
                    map[rs.getLong("userId")] = rs.getLong("money")
                }

                it.resume(map)
            },
            offset,
            users
        )
    }
}