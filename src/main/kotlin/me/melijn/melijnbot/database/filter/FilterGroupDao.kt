package me.melijn.melijnbot.database.filter

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FilterGroupDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "filterGroups"
    override val tableStructure: String = "guildId bigint, groupId int, channelIds varchar(2048), enabled boolean, points int"
    override val primaryKey: String = "guildId, groupId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun add(guildId: Long, group: FilterGroup) {
        group.apply {
            driverManager.executeUpdate("INSERT INTO $table (guildId, groupId, enabled, points) VALUES (?, ?, ?, ?)",
                guildId, groupId, enabled, points)
        }
    }

    suspend fun get(guildId: Long): List<FilterGroup> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val list = mutableListOf<FilterGroup>()
            while (rs.next()) {
                list.add(
                    FilterGroup(
                        rs.getInt("groupId"),
                        rs.getBoolean("enabled"),
                        rs.getInt("points")
                    )
                )
            }
            it.resume(list)
        }, guildId)
    }

    suspend fun remove(guildId: Long, group: FilterGroup) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND groupId = ?",
            guildId, group.groupId)
    }
}

data class FilterGroup(
    val groupId: Int,
    val enabled: Boolean,
    val points: Int
)