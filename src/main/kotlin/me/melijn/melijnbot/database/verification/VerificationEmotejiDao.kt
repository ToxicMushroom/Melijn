package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VerificationEmotejiDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "verificationEmotejis"
    override val tableStructure: String = "guildId bigint, emoteji varchar(64)"
    override val primaryKey: String = "guildId"

    override val cacheName: String = "verification:emoteji"

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

    fun set(guildId: Long, emoteji: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, emoteji) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET emoteji = ?",
            guildId, emoteji, emoteji)
    }

    fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }
}