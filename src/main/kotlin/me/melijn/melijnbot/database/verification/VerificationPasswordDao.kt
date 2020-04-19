package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VerificationPasswordDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "verificationPasswords"
    override val tableStructure: String = "guildId bigint, password varchar(128)"
    override val primaryKey: String = "guildId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(guildId: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("password"))
            } else {
                it.resume("")
            }
        }, guildId)
    }

    suspend fun set(guildId: Long, code: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, password) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET password = ?",
            guildId, code, code)
    }

    suspend fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }

}