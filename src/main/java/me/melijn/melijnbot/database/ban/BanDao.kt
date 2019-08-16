package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class BanDao(val driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "bans"
    override val tableStructure: String = "guildId bigint, bannedId bigint, authorId bigint, reason varchar(2048), startTime bigint, endTime bigint, unbanReason varchar(2048), active boolean"
    override val keys: String = "UNIQUE KEY(guildId, bannedId, startTime)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun addBan(ban: Ban) {
        ban.apply {
            driverManager.executeUpdate("INSERT INTO $table (guildId, bannedId, authorId, reason, startTime, endTime, unbanReason, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    guildId, bannedId, authorId, reason, startTime, endTime, unbanReason, active)
        }
    }

    fun getUnbannableBans(): List<Ban> {
        val bans = ArrayList<Ban>()
        driverManager.executeQuery("SELECT * FROM $table WHERE active = ? AND endTime < ?", { rs ->
            while (rs.next()) {
                bans.add(Ban(
                        rs.getLong("guildId"),
                        rs.getLong("bannedId"),
                        rs.getLong("authorId"),
                        rs.getNString("reason"),
                        rs.getNString("unbanReason"),
                        rs.getLong("startTime"),
                        rs.getLong("endTime"),
                        true
                ))
            }
        }, true, System.currentTimeMillis())
        return bans
    }
}

data class Ban(var guildId: Long,
               var bannedId: Long,
               var authorId: Long,
               var reason: String = "/",
               var unbanReason: String? = null,
               var startTime: Long = System.currentTimeMillis(),
               var endTime: Long? = null,
               var active: Boolean = true)