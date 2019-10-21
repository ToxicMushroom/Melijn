package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VerificationUserFlowRateDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "verificationUserFlowRates"
    override val tableStructure: String = "guildId bigint, rate bigint"
    override val keys: String = "PRIMARY KEY(guildId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun get(guildId: Long): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", {rs ->
            if (rs.next()) {
                it.resume(rs.getLong("rate"))
            } else {
                it.resume(-1)
            }
        }, guildId)
    }

    suspend fun set(guildId: Long, flowRate: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, rate) VALUES (?, ?) ON CONFLICT (guildId) DO UPDATE SET rate = ?",
            guildId, flowRate, flowRate)
    }

    suspend fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }


}