package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.VerificationType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VerificationTypeDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "verificationTypes"
    override val tableStructure: String = "guildId bigint, type varchar(64)"
    override val keys: String = "PRIMARY KEY(guildId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun get(guildId: Long): VerificationType = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", {rs ->
            if (rs.next()) {
                it.resume(VerificationType.valueOf(rs.getString("type")))
            } else {
                it.resume(VerificationType.NONE)
            }
        }, guildId)
    }

    suspend fun set(guildId: Long, type: VerificationType) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, type) VALUES (?, ?) ON DUPLICATE KEY UPDATE type = ?",
            guildId, type.toString(), type.toString())
    }

    suspend fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }


}