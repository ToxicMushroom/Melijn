package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EmbedColorDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "embedColors"
    override val tableStructure: String = "guildId bigint, color int"
    override val primaryKey: String = "guildId"

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

    suspend fun set(guildId: Long, color: Int) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, color) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET color = ?",
            guildId, color, color)
    }

    suspend fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ?",
            guildId)
    }
}