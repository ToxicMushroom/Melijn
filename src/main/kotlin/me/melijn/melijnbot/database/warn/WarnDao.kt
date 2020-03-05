package me.melijn.melijnbot.database.warn

import com.wrapper.spotify.Base64
import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.objects.utils.remove
import java.nio.ByteBuffer

class WarnDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "warns"
    override val tableStructure: String = "warnId varchar(16), " +
        "guildId bigint, warnedId bigint, warnAuthorId bigint, warnReason varchar(2000), warnMoment bigint"
    override val primaryKey: String = "warnId"
    override val uniqueKey: String = "guildId, warnedId, warnMoment"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey, uniqueKey)
    }

    suspend fun add(warn: Warn) {
        warn.apply {
            driverManager.executeUpdate("INSERT INTO $table (warnId, guildId, warnedId, warnAuthorId, warnReason, warnMoment) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
                warnId, guildId, warnedId, warnAuthorId, reason, moment)
        }
    }

    fun get(guildId: Long, warnedId: Long, warnMoment: Long, kick: (Warn) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND warnedId = ? AND warnMoment = ?", { rs ->
            kick.invoke(Warn(
                rs.getLong("guildId"),
                rs.getLong("warnedId"),
                rs.getLong("warnAuthorId"),
                rs.getString("warnReason"),
                rs.getLong("warnMoment"),
                rs.getString("warnId")
            ))
        }, guildId, warnedId, warnMoment)
    }


    fun getWarns(guildId: Long, warnedId: Long): List<Warn> {
        val kicks = ArrayList<Warn>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND warnedId = ?", { rs ->
            while (rs.next()) {
                kicks.add(Warn(
                    guildId,
                    warnedId,
                    rs.getLong("warnAuthorId"),
                    rs.getString("warnReason"),
                    rs.getLong("warnMoment"),
                    rs.getString("warnId")
                ))
            }
        }, guildId, warnedId)
        return kicks
    }

    fun getWarns(warnId: String): List<Warn> {
        val kicks = ArrayList<Warn>()
        driverManager.executeQuery("SELECT * FROM $table WHERE warnId = ?", { rs ->
            while (rs.next()) {
                kicks.add(Warn(
                    rs.getLong("guildId"),
                    rs.getLong("warnedId"),
                    rs.getLong("warnAuthorId"),
                    rs.getString("warnReason"),
                    rs.getLong("warnMoment"),
                    warnId
                ))
            }
        }, warnId)
        return kicks
    }
}

data class Warn(
    val guildId: Long,
    val warnedId: Long,
    val warnAuthorId: Long,
    val reason: String,
    val moment: Long = System.currentTimeMillis(),
    val warnId: String = Base64.encode(ByteBuffer
            .allocate(Long.SIZE_BYTES)
            .putLong(System.nanoTime())
            .array())
        .remove("=")
)