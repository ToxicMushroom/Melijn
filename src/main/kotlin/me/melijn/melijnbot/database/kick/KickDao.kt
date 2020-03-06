package me.melijn.melijnbot.database.kick

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.objects.utils.StringUtils.toBase64

class KickDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "kicks"
    override val tableStructure: String = "kickId varchar(16), " +
        "guildId bigint, kickedId bigint, kickAuthorId bigint, kickReason varchar(2000), kickMoment bigint"
    override val primaryKey: String = "kickId"
    override val uniqueKey: String = "guildId, kickedId, kickMoment"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey, uniqueKey)
    }

    suspend fun add(kick: Kick) {
        kick.apply {
            driverManager.executeUpdate("INSERT INTO $table (kickId, guildId, kickedId, kickAuthorId, kickReason, kickMoment) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
                kickId, guildId, kickedId, kickAuthorId, reason, moment)
        }
    }

    fun get(guildId: Long, kickedId: Long, kickMoment: Long, kick: (Kick) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND kickedId = ? AND kickMoment = ?", { rs ->
            kick.invoke(Kick(
                rs.getLong("guildId"),
                rs.getLong("kickedId"),
                rs.getLong("kickAuthorId"),
                rs.getString("kickReason"),
                rs.getLong("kickMoment"),
                rs.getString("kickId")
            ))
        }, guildId, kickedId, kickMoment)
    }


    fun getKicks(guildId: Long, kickedId: Long): List<Kick> {
        val kicks = ArrayList<Kick>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND kickedId = ?", { rs ->
            while (rs.next()) {
                kicks.add(Kick(
                    guildId,
                    kickedId,
                    rs.getLong("kickAuthorId"),
                    rs.getString("kickReason"),
                    rs.getLong("kickMoment"),
                    rs.getString("kickId")
                ))
            }
        }, guildId, kickedId)
        return kicks
    }

    fun getKicks(kickId: String): List<Kick> {
        val kicks = ArrayList<Kick>()
        driverManager.executeQuery("SELECT * FROM $table WHERE kickId = ?", { rs ->
            while (rs.next()) {
                kicks.add(Kick(
                    rs.getLong("guildId"),
                    rs.getLong("kickedId"),
                    rs.getLong("kickAuthorId"),
                    rs.getString("kickReason"),
                    rs.getLong("kickMoment"),
                    kickId
                ))
            }
        }, kickId)
        return kicks
    }
}

data class Kick(
    val guildId: Long,
    val kickedId: Long,
    val kickAuthorId: Long,
    val reason: String,
    val moment: Long = System.currentTimeMillis(),
    val kickId: String = System.nanoTime().toBase64()
)