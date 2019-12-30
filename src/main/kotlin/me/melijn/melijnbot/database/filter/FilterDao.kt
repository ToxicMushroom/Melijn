package me.melijn.melijnbot.database.filter

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.FilterType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FilterDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "filters"
    override val tableStructure: String = "guildId bigint, filterName bigint, type varchar(32), filter varchar(2048)"
    override val primaryKey: String = "guildId, filterName, type, filter"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun add(guildId: Long, filterName: String, type: FilterType, filter: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, filterName, type, filter) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            guildId, filterName, type.toString(), filter)
    }

    suspend fun get(guildId: Long, filterName: String, type: FilterType): List<String> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND channelId = ? AND type = ? ", { rs ->
            val filters = mutableListOf<String>()
            while (rs.next()) {
                filters.add(rs.getString("filter"))
            }
            it.resume(filters)
        }, guildId, filterName, type.toString())
    }

    suspend fun remove(guildId: Long, filterName: String, type: FilterType, filter: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND groupId = ? AND type = ? AND filter = ?",
            guildId, filterName, type.toString(), filter)
    }
}