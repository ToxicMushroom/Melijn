package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UnverifiedUsersDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "unverifiedUsers"
    override val tableStructure: String = "guildId bigint UNIQUE, userId bigint UNIQUE, moment bigint, triesAmount int"
    override val keys: String = ""

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun add(guildId: Long, userId: Long) {
        driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, userId, moment, triesAmount) VALUES (?, ?, ?, ?)",
            guildId, userId, 0, 0)
    }

    suspend fun remove(guildId: Long, userId: Long) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND userId = ?",
            guildId, userId)
    }

    suspend fun getMoment(guildId: Long, userId: Long): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND userId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getLong("moment"))
            } else {
                it.resume(0)
            }
        }, guildId, userId)
    }

    suspend fun getTries(guildId: Long, userId: Long): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND userId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getLong("triesAmount"))
            } else {
                it.resume(0)
            }
        }, guildId, userId)
    }

    suspend fun contains(guildId: Long, userId: Long): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND userId = ?", { rs ->
            it.resume(rs.next())
        }, guildId, userId)
    }

    suspend fun update(guildId: Long, userId: Long, tries: Long) {
        driverManager.executeUpdate("UPDATE $table SET triesAmount = ? WHERE guildId = ? AND userId = ?",
            tries, guildId, userId)
    }


}