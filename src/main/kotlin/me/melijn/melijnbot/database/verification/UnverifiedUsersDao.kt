package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UnverifiedUsersDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "unverifiedUsers"
    override val tableStructure: String = "guildId bigint, userId bigint, code bigint"
    override val keys: String = "UNIQUE KEY(guildId, userId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun add(guildId: Long, userId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, userId, code) VALUES (?, ?, ?)",
            guildId, userId, 0)
    }

    suspend fun remove(guildId: Long, userId: Long) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND userId = ?",
            guildId, userId)
    }

    suspend fun getCode(guildId: Long, userId: Long): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND userId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getLong("code"))
            } else {
                it.resume(0)
            }
        }, guildId, userId)
    }


}