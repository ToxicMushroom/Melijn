package me.melijn.melijnbot.database.economy

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BalanceDao(driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "balances"
    override val tableStructure: String = "userId bigint, money bigint"
    override val primaryKey: String = "userId"

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

    suspend fun setBalance(userId: Long, money: Long) {
        driverManager.executeUpdate("INSERT INTO $table (userId, money) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET money=?",
            userId, money, money)
    }

    suspend fun getTop(users: Int, offset: Int): Map<Long, Long> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table ORDER BY money DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", { rs ->
            val map = mutableMapOf<Long, Long>()

            while (rs.next()) {
                map[rs.getLong("userId")] = rs.getLong("money")
            }

            it.resume(map)
        }, offset, users)
    }
}