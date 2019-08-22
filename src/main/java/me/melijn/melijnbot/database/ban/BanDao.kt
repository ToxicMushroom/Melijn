package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class BanDao(val driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "bans"
    override val tableStructure: String = "guildId bigint, bannedId bigint, banAuthorId bigint, unbanAuthorId bigint, reason varchar(2048), startTime bigint, endTime bigint, unbanReason varchar(2048), active boolean"
    override val keys: String = "UNIQUE KEY(guildId, bannedId, startTime)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun setBan(ban: Ban) {
        ban.apply {
            driverManager.executeUpdate("INSERT INTO $table (guildId, bannedId, banAuthorId, unbanAuthorId, reason, startTime, endTime, unbanReason, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE endTime = ?, unbanReason = ?, active = ?",
                    guildId, bannedId, banAuthorId, unbanAuthorId, reason, startTime, endTime, unbanReason, active, endTime, unbanReason, active)
        }
    }

    fun getUnbannableBans(): List<Ban> {
        val bans = ArrayList<Ban>()
        driverManager.executeQuery("SELECT * FROM $table WHERE active = ? AND endTime < ?", { rs ->
            while (rs.next()) {
                bans.add(Ban(
                        rs.getLong("guildId"),
                        rs.getLong("bannedId"),
                        rs.getLong("banAuthorId"),
                        rs.getNString("reason"),
                        rs.getLong("unbanAuthorId"),
                        rs.getNString("unbanReason"),
                        rs.getLong("startTime"),
                        rs.getLong("endTime"),
                        true
                ))
            }
        }, true, System.currentTimeMillis())
        return bans
    }

    fun getActiveBan(guildId: Long, bannedId: Long): Ban? {
        var ban: Ban? = null
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND bannedId = ? AND active = ?", { rs ->
            while (rs.next()) {
                ban = Ban(
                        guildId,
                        bannedId,
                        rs.getLong("banAuthorId"),
                        rs.getNString("reason"),
                        rs.getLong("unbanAuthorId"),
                        rs.getNString("unbanReason"),
                        rs.getLong("startTime"),
                        rs.getLong("endTime"),
                        true
                )
            }
        }, guildId, bannedId, true)
        return ban
    }
}

data class Ban(var guildId: Long,
               var bannedId: Long,
               var banAuthorId: Long?,
               var reason: String = "/",
               var unbanAuthorId: Long? = null,
               var unbanReason: String? = null,
               var startTime: Long = System.currentTimeMillis(),
               var endTime: Long? = null,
               var active: Boolean = true)