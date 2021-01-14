package me.melijn.melijnbot.database.kick

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.utils.StringUtils.toBase64
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
            driverManager.executeUpdate(
                "INSERT INTO $table (kickId, guildId, kickedId, kickAuthorId, kickReason, kickMoment) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
                kickId, guildId, kickedId, kickAuthorId, reason, moment
            )
        }
    }

    fun get(guildId: Long, kickedId: Long, kickMoment: Long, kick: (Kick) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND kickedId = ? AND kickMoment = ?", { rs ->
            kick.invoke(
                Kick(
                    rs.getLong("guildId"),
                    rs.getLong("kickedId"),
                    rs.getLong("kickAuthorId"),
                    rs.getString("kickReason"),
                    rs.getLong("kickMoment"),
                    rs.getString("kickId")
                )
            )
        }, guildId, kickedId, kickMoment)
    }


    suspend fun getKicks(guildId: Long, kickedId: Long): List<Kick> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND kickedId = ?", { rs ->
            val kicks = mutableListOf<Kick>()
            while (rs.next()) {
                kicks.add(
                    Kick(
                        guildId,
                        kickedId,
                        rs.getLong("kickAuthorId"),
                        rs.getString("kickReason"),
                        rs.getLong("kickMoment"),
                        rs.getString("kickId")
                    )
                )
            }
            it.resume(kicks)
        }, guildId, kickedId)
    }

    suspend fun getKicks(kickId: String): List<Kick> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE kickId = ?", { rs ->
            val kicks = mutableListOf<Kick>()
            while (rs.next()) {
                kicks.add(
                    Kick(
                        rs.getLong("guildId"),
                        rs.getLong("kickedId"),
                        rs.getLong("kickAuthorId"),
                        rs.getString("kickReason"),
                        rs.getLong("kickMoment"),
                        kickId
                    )
                )
            }
            it.resume(kicks)
        }, kickId)
    }

    suspend fun clear(guildId: Long, kickedId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guildId = ? AND kickedId = ?",
            guildId, kickedId
        )
    }

    suspend fun remove(kick: Kick) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guildId = ? AND kickedId = ? AND kickId = ?",
            kick.guildId, kick.kickedId, kick.kickId
        )
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