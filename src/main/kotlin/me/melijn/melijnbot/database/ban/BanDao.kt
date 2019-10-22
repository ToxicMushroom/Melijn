package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BanDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "bans"
    override val tableStructure: String = "" +
        "guildId bigint, bannedId bigint, banAuthorId bigint, " +
        "unbanAuthorId bigint, reason varchar(2048), startTime bigint, endTime bigint," +
        " unbanReason varchar(2048), active boolean"
    override val keys: String = "PRIMARY KEY (guildId, bannedId, startTime)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun setBan(ban: Ban) {
        ban.apply {
            driverManager.executeUpdate("INSERT INTO $table (guildId, bannedId, banAuthorId, unbanAuthorId, reason, startTime, endTime, unbanReason, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                " ON CONFLICT (guildId, bannedId, startTime) DO UPDATE SET endTime = ?, banAuthorId = ?, reason = ?, unbanAuthorId = ?, unbanReason = ?, active = ?",
                guildId, bannedId, banAuthorId, unbanAuthorId, reason, startTime, endTime, unbanReason, active,
                endTime, banAuthorId, reason, unbanAuthorId, unbanReason, active)
        }
    }

    suspend fun getUnbannableBans(): List<Ban> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE active = ? AND endTime < ?", { rs ->
            val bans = ArrayList<Ban>()
            while (rs.next()) {
                bans.add(Ban(
                    rs.getLong("guildId"),
                    rs.getLong("bannedId"),
                    rs.getLong("banAuthorId"),
                    rs.getString("reason"),
                    rs.getLong("unbanAuthorId"),
                    rs.getString("unbanReason"),
                    rs.getLong("startTime"),
                    rs.getLong("endTime"),
                    true
                ))
            }
            it.resume(bans)
        }, true, System.currentTimeMillis())
    }

    suspend fun getActiveBan(guildId: Long, bannedId: Long): Ban? = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE guildId = ? AND bannedId = ? AND active = ?", { rs ->
            var ban: Ban? = null
            while (rs.next()) {
                ban = Ban(
                    guildId,
                    bannedId,
                    rs.getLong("banAuthorId"),
                    rs.getString("reason"),
                    rs.getLong("unbanAuthorId"),
                    rs.getString("unbanReason"),
                    rs.getLong("startTime"),
                    rs.getLong("endTime"),
                    true
                )
            }
            it.resume(ban)
        }, guildId, bannedId, true)
    }

    suspend fun getBans(guildId: Long, bannedId: Long): List<Ban> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE guildId = ? AND bannedId = ?", { rs ->
            val bans = ArrayList<Ban>()
            while (rs.next()) {
                bans.add(Ban(
                    guildId,
                    bannedId,
                    rs.getLong("banAuthorId"),
                    rs.getString("reason"),
                    rs.getLong("unbanAuthorId"),
                    rs.getString("unbanReason"),
                    rs.getLong("startTime"),
                    rs.getLong("endTime"),
                    rs.getBoolean("active")
                ))
            }
            it.resume(bans)
        }, guildId, bannedId)
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