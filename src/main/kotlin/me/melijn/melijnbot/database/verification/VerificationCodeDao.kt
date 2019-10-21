package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VerificationCodeDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "verificationCodes"
    override val tableStructure: String = "guildId bigint, code varchar(64)"
    override val keys: String = "PRIMARY KEY(guildId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun get(guildId: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", {rs ->
            if (rs.next()) {
                it.resume(rs.getString("code"))
            } else {
                it.resume("")
            }
        }, guildId)
    }

    suspend fun set(guildId: Long, code: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, code) VALUES (?, ?) ON CONFLICT (guildId) DO UPDATE SET code = ?",
            guildId, code, code)
    }

    suspend fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }


}