package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DenyVoteReminderDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "deniedVoteReminders"
    override val tableStructure: String = "userId bigint"
    override val primaryKey: String = "userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun add(userId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (userId) VALUES (?) ON CONFLICT ($primaryKey) DO NOTHING",
            userId)
    }

    suspend fun contains(userId: Long): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { rs ->
            it.resume(rs.next())
        }, userId)
    }

    suspend fun remove(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ?", userId)
    }
}