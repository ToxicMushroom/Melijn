package me.melijn.melijnbot.database.channel

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MusicChannelDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "musicChannel"
    override val tableStructure: String = "guildId bigint, channelId bigint"
    override val primaryKey: String = "guildId"

    override val cacheName: String = "channel:music"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(guildId: Long, channelId: Long) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guildId, channelId) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET channelId = ?",
            guildId, channelId, channelId
        )
    }

    fun remove(guildId: Long) {
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