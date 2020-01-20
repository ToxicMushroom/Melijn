package me.melijn.melijnbot.database.filter

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.FilterMode
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FilterGroupDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "filterGroups"
    override val tableStructure: String = "guildId bigint, filterGroupName varchar(32), channelIds varchar(2048), mode varchar(64), state boolean, points int"
    override val primaryKey: String = "guildId, filterGroupName"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun add(guildId: Long, group: FilterGroup) {
        group.apply {
            val query = "INSERT INTO $table (guildId, filterGroupName, channelIds, mode, state, points) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT ($primaryKey) DO UPDATE SET channelIds = ?, mode = ?, state = ?, points = ?"
            driverManager.executeUpdate(query,
                guildId,
                filterGroupName,
                group.channels.joinToString(","),
                mode.toString(), state, points,
                group.channels.joinToString(","),
                mode.toString(), state, points
            )
        }
    }

    suspend fun get(guildId: Long): List<FilterGroup> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val list = mutableListOf<FilterGroup>()
            while (rs.next()) {
                val channels = rs.getString("channelIds")

                list.add(
                    FilterGroup(
                        rs.getString("filterGroupName"),
                        rs.getBoolean("state"),
                        if (channels.isBlank())
                            longArrayOf()
                        else channels.split(",")
                            .map { id ->
                                id.toLong()
                            }
                            .toLongArray(),
                        FilterMode.valueOf(rs.getString("mode")),
                        rs.getInt("points")
                    )
                )
            }
            it.resume(list)
        }, guildId)
    }

    suspend fun remove(guildId: Long, group: FilterGroup) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND filterGroupName = ?",
            guildId, group.filterGroupName)
    }
}

data class FilterGroup(
    val filterGroupName: String,
    var state: Boolean,
    var channels: LongArray,
    var mode: FilterMode,
    var points: Int
)