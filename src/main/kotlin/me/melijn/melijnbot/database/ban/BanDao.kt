package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class BanDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "bans"
    override val tableStructure: String = "guildId bigint, bannedId bigint, banAuthorId bigint, unbanAuthorId bigint, reason varchar(2048), startTime bigint, endTime bigint, unbanReason varchar(2048), active boolean"
    override val keys: String = "UNIQUE KEY(guildId, bannedId, startTime)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun setBan(ban: Ban) {
        ban.apply {
            driverManager.executeUpdate("INSERT INTO $table (guildId, bannedId, banAuthorId, unbanAuthorId, reason, startTime, endTime, unbanReason, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE endTime = ?, banAuthorId = ?, reason = ?, unbanAuthorId = ?, unbanReason = ?, active = ?",
                guildId, bannedId, banAuthorId, unbanAuthorId, reason, startTime, endTime, unbanReason, active,
                endTime, banAuthorId, reason, unbanAuthorId, unbanReason, active)
        }
    }

    suspend fun getUnbannableBans(): List<Ban> {
        driverManager.awaitQueryExecution(
            "SELECT * FROM $table WHERE active = ? AND endTime < ?",
            true, System.currentTimeMillis()
        ).use { rs ->
            val bans = ArrayList<Ban>()
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
            return bans
        }
    }

    suspend fun getActiveBan(guildId: Long, bannedId: Long): Ban? {
        var ban: Ban? = null
        driverManager.awaitQueryExecution(
            "SELECT * FROM $table WHERE guildId = ? AND bannedId = ? AND active = ?",
            guildId, bannedId, true
        ).use { rs ->
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
        }
        return ban
    }

    suspend fun getBans(guildId: Long, bannedId: Long): List<Ban> {
        val bans = ArrayList<Ban>()
        driverManager.awaitQueryExecution(
            "SELECT * FROM $table WHERE guildId = ? AND bannedId = ?",
            guildId, bannedId
        ).use { rs ->
            while (rs.next()) {
                bans.add(Ban(
                    guildId,
                    bannedId,
                    rs.getLong("banAuthorId"),
                    rs.getNString("reason"),
                    rs.getLong("unbanAuthorId"),
                    rs.getNString("unbanReason"),
                    rs.getLong("startTime"),
                    rs.getLong("endTime"),
                    rs.getBoolean("active")
                ))
            }
        }
        return bans
    }
}

data class Ban(
    var guildId: Long,
    var bannedId: Long,
    var banAuthorId: Long?,
    var reason: String = "/",
    var unbanAuthorId: Long? = null,
    var unbanReason: String? = null,
    var startTime: Long = System.currentTimeMillis(),
    var endTime: Long? = null,
    var active: Boolean = true
)