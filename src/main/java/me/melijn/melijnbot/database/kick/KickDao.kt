package me.melijn.melijnbot.database.kick

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class KickDao(val driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "kick"
    override val tableStructure: String = "guildId bigint, kickedId bigint, kickAuthorId bigint, kickReason varchar(64), kickMoment bigint"
    override val keys: String = "UNIQUE KEY (guildId, kickedId, kickMoment)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun add(kick: Kick) {
        kick.apply {
            driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, kickedId, kickAuthorId, kickReason, kickMoment) VALUES (?, ?, ?, ?, ?)",
                    guildId, kickedId, kickAuthorId, kickReason, kickMoment)
        }

    }

    fun get(guildId: Long, kickedId: Long, kickMoment: Long, kick: (Kick) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND kickedId = ? AND kickMoment = ?", { rs ->
            kick.invoke(Kick(
                    rs.getLong("guildId"),
                    rs.getLong("kickedId"),
                    rs.getLong("kickAuthorId"),
                    rs.getString("kickReason"),
                    rs.getLong("kickMoment")
            ))
        }, guildId, kickedId, kickMoment)
    }
}

data class Kick(
        val guildId: Long,
        val kickedId: Long,
        val kickAuthorId: Long,
        val kickReason: String,
        val kickMoment: Long = System.currentTimeMillis()
)