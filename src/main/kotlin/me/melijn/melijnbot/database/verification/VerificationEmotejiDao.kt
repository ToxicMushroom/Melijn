package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VerificationEmotejiDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "verificationEmotejis"
    override val tableStructure: String = "guildId bigint, emoteji varchar(64)"
    override val primaryKey: String = "guildId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(guildId: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("emoteji"))
            } else {
                it.resume("")
            }
        }, guildId)
    }

    suspend fun set(guildId: Long, emoteji: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, emoteji) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET emoteji = ?",
            guildId, emoteji, emoteji)
    }

    suspend fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }


}