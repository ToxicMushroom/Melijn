package me.melijn.melijnbot.database.mute

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.database.ban.TempPunishment
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.utils.StringUtils.toBase64
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MuteDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "mutes"
    override val tableStructure: String = "muteId varchar(16), " +
            "guildId bigint, mutedId bigint, muteAuthorId bigint," +
            " unmuteAuthorId bigint, reason varchar(2048), startTime bigint, endTime bigint," +
            " unmuteReason varchar(2048), active boolean"
    override val primaryKey: String = "muteId"
    override val uniqueKey: String = "guildId, mutedId, startTime"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey, uniqueKey)
    }

    fun setMute(mute: Mute) {
        mute.apply {
            driverManager.executeUpdate(
                "INSERT INTO $table (muteId, guildId, mutedId, muteAuthorId, unmuteAuthorId, reason, startTime, endTime, unmuteReason, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                        " ON CONFLICT ($primaryKey) DO UPDATE SET endTime = ?, muteAuthorId = ?, reason = ?, unmuteAuthorId = ?, unmuteReason = ?, active = ?",
                muteId,
                guildId,
                mutedId,
                muteAuthorId,
                unmuteAuthorId,
                reason,
                startTime,
                endTime,
                unmuteReason,
                active,
                endTime,
                muteAuthorId,
                reason,
                unmuteAuthorId,
                unmuteReason,
                active
            )
        }
    }

    suspend fun getUnmuteableMutes(podInfo: PodInfo): List<Mute> = suspendCoroutine {
        val clause = podInfo.shardList.joinToString(", ") { "?" }
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE active = ? AND endTime < ? AND ((guildId >> 22) % ${podInfo.shardCount}) IN ($clause)",
            { rs ->
                val mutes = ArrayList<Mute>()
                while (rs.next()) {
                    mutes.add(
                        Mute(
                            rs.getLong("guildId"),
                            rs.getLong("mutedId"),
                            rs.getLong("muteAuthorId"),
                            rs.getString("reason"),
                            rs.getLong("unmuteAuthorId"),
                            rs.getString("unmuteReason"),
                            rs.getLong("startTime"),
                            rs.getLong("endTime"),
                            true,
                            rs.getString("muteId")
                        )
                    )
                }
                it.resume(mutes)
            },
            true,
            System.currentTimeMillis(),
            *podInfo.shardList.toTypedArray()
        )
    }

    suspend fun getActiveMute(guildId: Long, mutedId: Long): Mute? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND mutedId = ? AND active = ?", { rs ->
            if (rs.next()) {
                val mute = Mute(
                    guildId,
                    mutedId,
                    rs.getLong("muteAuthorId"),
                    rs.getString("reason"),
                    rs.getLong("unmuteAuthorId"),
                    rs.getString("unmuteReason"),
                    rs.getLong("startTime"),
                    rs.getLong("endTime"),
                    true,
                    rs.getString("muteId")
                )
                it.resume(mute)
            } else {
                it.resume(null)
            }
        }, guildId, mutedId, true)
    }

    suspend fun getMutes(guildId: Long, mutedId: Long): List<Mute> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND mutedId = ?", { rs ->
            val mutes = mutableListOf<Mute>()
            while (rs.next()) {
                mutes.add(
                    Mute(
                        guildId,
                        mutedId,
                        rs.getLong("muteAuthorId"),
                        rs.getString("reason"),
                        rs.getLong("unmuteAuthorId"),
                        rs.getString("unmuteReason"),
                        rs.getLong("startTime"),
                        rs.getLong("endTime"),
                        rs.getBoolean("active"),
                        rs.getString("muteId")
                    )
                )
            }
            it.resume(mutes)
        }, guildId, mutedId)
    }

    suspend fun getMutes(muteId: String): List<Mute> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE muteId = ?", { rs ->
            val mutes = ArrayList<Mute>()
            while (rs.next()) {
                mutes.add(
                    Mute(
                        rs.getLong("guildId"),
                        rs.getLong("mutedId"),
                        rs.getLong("muteAuthorId"),
                        rs.getString("reason"),
                        rs.getLong("unmuteAuthorId"),
                        rs.getString("unmuteReason"),
                        rs.getLong("startTime"),
                        rs.getLong("endTime"),
                        rs.getBoolean("active"),
                        muteId
                    )
                )
            }
            it.resume(mutes)
        }, muteId)
    }

    fun clearHistory(guildId: Long, mutedId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guildId = ? AND mutedId = ? AND active = ?",
            guildId, mutedId, false
        )
    }

    fun clear(guildId: Long, mutedId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guildId = ? AND mutedId = ?",
            guildId, mutedId
        )
    }

    fun remove(mute: Mute) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guildId = ? AND mutedId = ? AND muteId = ?",
            mute.guildId, mute.mutedId, mute.muteId
        )
    }
}

data class Mute(
    override var guildId: Long,
    var mutedId: Long,
    var muteAuthorId: Long?,
    override var reason: String = "/",
    var unmuteAuthorId: Long? = null,
    var unmuteReason: String? = null,
    override var startTime: Long = System.currentTimeMillis(),
    override var endTime: Long? = null,
    override var active: Boolean = true,
    var muteId: String = System.nanoTime().toBase64()
) : TempPunishment(
    guildId,
    mutedId,
    muteAuthorId,
    reason,
    unmuteAuthorId,
    unmuteReason,
    startTime,
    endTime,
    active,
    muteId
)