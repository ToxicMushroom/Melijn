package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.objects.utils.StringUtils.toBase64
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SoftBanDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "softBans"
    override val tableStructure: String = "softBanId varchar(16), " +
        "guildId bigint, softBannedId bigint, softBanAuthorId bigint, reason varchar(2048), moment bigint"
    override val primaryKey: String = "softBanId"
    override val uniqueKey: String = "guildId, softBannedId, moment"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey, uniqueKey)
    }

    suspend fun addSoftBan(ban: SoftBan) {
        ban.apply {
            driverManager.executeUpdate("INSERT INTO $table (softBanId, guildId, softBannedId, softBanAuthorId, reason, moment) VALUES (?, ?, ?, ?, ?, ?)" +
                " ON CONFLICT ($primaryKey) DO NOTHING",
                softBanId, guildId, softBannedId, softBanAuthorId, reason, moment)
        }
    }

    suspend fun getSoftBans(guildId: Long, bannedId: Long): List<SoftBan> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE guildId = ? AND softBannedId = ?", { rs ->
            val softBans = ArrayList<SoftBan>()
            while (rs.next()) {
                softBans.add(SoftBan(
                    guildId,
                    bannedId,
                    rs.getLong("softBanAuthorId"),
                    rs.getString("reason"),
                    rs.getLong("moment"),
                    rs.getString("softBanId")
                ))
            }
            it.resume(softBans)
        }, guildId, bannedId)
    }

    suspend fun getSoftBans(softbanId: String): List<SoftBan> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE softbanId = ?", { rs ->
            val softBans = ArrayList<SoftBan>()
            while (rs.next()) {
                softBans.add(SoftBan(
                    rs.getLong("guildId"),
                    rs.getLong("bannedId"),
                    rs.getLong("softBanAuthorId"),
                    rs.getString("reason"),
                    rs.getLong("moment"),
                    softbanId
                ))
            }
            it.resume(softBans)
        }, softbanId)
    }

}

data class SoftBan(
    var guildId: Long,
    var softBannedId: Long,
    var softBanAuthorId: Long,
    var reason: String = "/",
    var moment: Long = System.currentTimeMillis(),
    val softBanId: String = System.nanoTime().toBase64()
)