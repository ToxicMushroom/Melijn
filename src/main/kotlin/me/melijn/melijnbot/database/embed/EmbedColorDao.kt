package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EmbedColorDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "embedColors"
    override val tableStructure: String = "guildId bigint, color int"
    override val primaryKey: String = "guildId"

    override val cacheName: String = "embed:color"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(guildId: Long): Int = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getInt("color"))
            } else {
                it.resume(0)
            }
        }, guildId)
    }

    fun set(guildId: Long, color: Int) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guildId, color) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET color = ?",
            guildId, color, color
        )
    }

    fun remove(guildId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guildId = ?",
            guildId
        )
    }
}