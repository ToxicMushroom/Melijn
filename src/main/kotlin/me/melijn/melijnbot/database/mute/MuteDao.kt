package me.melijn.melijnbot.database.mute

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class MuteDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "mutes"
    override val tableStructure: String = "" +
        "guildId bigint, mutedId bigint, muteAuthorId bigint," +
        " unmuteAuthorId bigint, reason varchar(2048), startTime bigint, endTime bigint," +
        " unmuteReason varchar(2048), active boolean"
    override val keys: String = "UNIQUE (guildId, mutedId, startTime)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun setMute(mute: Mute) {
        mute.apply {
            driverManager.executeUpdate("INSERT INTO $table (guildId, mutedId, muteAuthorId, unmuteAuthorId, reason, startTime, endTime, unmuteReason, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                " ON CONFLICT (guildId, mutedId, startTime) DO UPDATE SET endTime = ?, muteAuthorId = ?, reason = ?, unmuteAuthorId = ?, unmuteReason = ?, active = ?",
                guildId, mutedId, muteAuthorId, unmuteAuthorId, reason, startTime, endTime, unmuteReason, active,
                endTime, muteAuthorId, reason, unmuteAuthorId, unmuteReason, active)
        }
    }

    fun getUnmuteableMutes(): List<Mute> {
        val mutes = ArrayList<Mute>()
        driverManager.executeQuery("SELECT * FROM $table WHERE active = ? AND endTime < ?", { rs ->
            while (rs.next()) {
                mutes.add(Mute(
                    rs.getLong("guildId"),
                    rs.getLong("mutedId"),
                    rs.getLong("muteAuthorId"),
                    rs.getString("reason"),
                    rs.getLong("unmuteAuthorId"),
                    rs.getString("unmuteReason"),
                    rs.getLong("startTime"),
                    rs.getLong("endTime"),
                    true
                ))
            }
        }, true, System.currentTimeMillis())
        return mutes
    }

    fun getActiveMute(guildId: Long, mutedId: Long): Mute? {
        var mute: Mute? = null
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND mutedId = ? AND active = ?", { rs ->
            while (rs.next()) {
                mute = Mute(
                    guildId,
                    mutedId,
                    rs.getLong("muteAuthorId"),
                    rs.getString("reason"),
                    rs.getLong("unmuteAuthorId"),
                    rs.getString("unmuteReason"),
                    rs.getLong("startTime"),
                    rs.getLong("endTime"),
                    true
                )
            }
        }, guildId, mutedId, true)
        return mute
    }

    fun getMutes(guildId: Long, mutedId: Long): List<Mute> {
        val mutes = ArrayList<Mute>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND mutedId = ?", { rs ->
            while (rs.next()) {
                mutes.add(Mute(
                    guildId,
                    mutedId,
                    rs.getLong("muteAuthorId"),
                    rs.getString("reason"),
                    rs.getLong("unmuteAuthorId"),
                    rs.getString("unmuteReason"),
                    rs.getLong("startTime"),
                    rs.getLong("endTime"),
                    rs.getBoolean("active")
                ))
            }
        }, guildId, mutedId)
        return mutes
    }
}

data class Mute(var guildId: Long,
                var mutedId: Long,
                var muteAuthorId: Long?,
                var reason: String = "/",
                var unmuteAuthorId: Long? = null,
                var unmuteReason: String? = null,
                var startTime: Long = System.currentTimeMillis(),
                var endTime: Long? = null,
                var active: Boolean = true)