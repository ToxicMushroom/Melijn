package me.melijn.melijnbot.database.warn

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.objects.utils.StringUtils.toBase64
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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


    suspend fun getWarns(guildId: Long, warnedId: Long): List<Warn> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND warnedId = ?", { rs ->
            val kicks = mutableListOf<Warn>()
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
            it.resume(kicks)
        }, guildId, warnedId)
    }

    suspend fun getWarns(warnId: String): List<Warn> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE warnId = ?", { rs ->
            val kicks = mutableListOf<Warn>()
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
            it.resume(kicks)
        }, warnId)
    }

    suspend fun clear(guildId: Long, warnedId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND warnedId = ?",
            guildId, warnedId)
    }

    suspend fun remove(warn: Warn) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND warnedId = ? AND warnId = ?",
            warn.guildId, warn.warnedId, warn.warnId)
    }
}

data class Warn(
    val guildId: Long,
    val warnedId: Long,
    val warnAuthorId: Long,
    val reason: String,
    val moment: Long = System.currentTimeMillis(),
    val warnId: String = System.nanoTime().toBase64()
)