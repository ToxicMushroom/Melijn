package me.melijn.melijnbot.database.autopunishment

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SpamGroupDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "spamGroups"
    override val tableStructure: String = "guildId bigint, spamGroupName varchar(32), channelIds varchar(2048), state boolean, points int"
    override val primaryKey: String = "guildId, spamGroupName"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun add(guildId: Long, group: SpamGroup) {
        group.apply {
            val query = "INSERT INTO $table (guildId, spamGroupName, channelIds, state, points) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT ($primaryKey) DO UPDATE SET channelIds = ?, state = ?, points = ?"
            driverManager.executeUpdate(query,
                guildId,
                spamGroupName,
                group.channels.joinToString(","),
                state, points,
                group.channels.joinToString(","),
                state, points
            )
        }
    }

    suspend fun get(guildId: Long): List<SpamGroup> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val list = mutableListOf<SpamGroup>()
            while (rs.next()) {
                val channels = rs.getString("channelIds")

                list.add(
                    SpamGroup(
                        rs.getString("spamGroupName"),
                        rs.getBoolean("state"),
                        if (channels.isBlank()) {
                            longArrayOf()
                        } else {
                            channels.split(",")
                                .map { id ->
                                    id.toLong()
                                }
                                .toLongArray()
                        },
                        rs.getInt("points")
                    )
                )
            }
            it.resume(list)
        }, guildId)
    }

    suspend fun remove(guildId: Long, group: SpamGroup) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND spamGroupName = ?",
            guildId, group.spamGroupName)
    }
}

data class SpamGroup(
    val spamGroupName: String,
    var state: Boolean,
    var channels: LongArray,
    var points: Int
)