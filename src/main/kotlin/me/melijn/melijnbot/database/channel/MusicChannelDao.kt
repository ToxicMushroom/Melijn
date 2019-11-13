package me.melijn.melijnbot.database.channel

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MusicChannelDao(driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "musicChannel"
    override val tableStructure: String = "guildId bigint, channelId bigint"
    override val primaryKey: String = "guildId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun set(guildId: Long, channelId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelId) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET channelId = ?",
            guildId, channelId, channelId)
    }

    suspend fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }

    suspend fun get(guildId: Long): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getLong("channelId"))
            } else {
                it.resume(-1L)
            }
        }, guildId)
    }
}