package me.melijn.melijnbot.database.filter

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FilterGroupDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "filterGroups"
    override val tableStructure: String = "guildId bigint, filterGroupName varchar(32), channelIds varchar(2048), state boolean, points int"
    override val primaryKey: String = "guildId, filterGroupName"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun add(guildId: Long, group: FilterGroup) {
        group.apply {
            driverManager.executeUpdate("INSERT INTO $table (guildId, filterGroupName, channelIds, state, points) VALUES (?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET channelIds = ?, state = ?, points = ?",
                guildId, filterGroupName, group.channels.joinToString(","), state, points, group.channels.joinToString(","), state, points)
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
    var points: Int
)