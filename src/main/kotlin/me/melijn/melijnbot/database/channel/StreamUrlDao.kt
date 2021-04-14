package me.melijn.melijnbot.database.channel

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class StreamUrlDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "streamUrls"
    override val tableStructure: String = "guildId bigint, url varchar(512)"
    override val primaryKey: String = "guildId"

    override val cacheName: String = "streamurl"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(guildId: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("url"))
            } else {
                it.resume("")
            }
        }, guildId)
    }

    fun set(guildId: Long, url: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, url) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET url = ?",
            guildId, url, url)
    }

    fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }
}