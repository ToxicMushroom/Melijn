package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MusicNodeDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "musicNodes"
    override val tableStructure: String = "guildId bigint, node varchar(32)"
    override val primaryKey: String = "guildId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun insert(guildId: Long, node: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, node) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET node = ?",
            guildId, node, node)
    }

    suspend fun get(guildId: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("node"))
            } else it.resume("")
        }, guildId)
    }
}