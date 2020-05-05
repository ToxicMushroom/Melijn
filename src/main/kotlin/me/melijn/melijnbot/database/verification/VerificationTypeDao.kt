package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.VerificationType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VerificationTypeDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "verificationTypes"
    override val tableStructure: String = "guildId bigint, type varchar(64)"
    override val primaryKey: String = "guildId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(guildId: Long): VerificationType = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            if (rs.next()) {
                it.resume(VerificationType.valueOf(rs.getString("type")))
            } else {
                it.resume(VerificationType.NONE)
            }
        }, guildId)
    }

    suspend fun set(guildId: Long, type: VerificationType) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, type) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET type = ?",
            guildId, type.toString(), type.toString())
    }

    suspend fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }


}