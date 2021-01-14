package me.melijn.melijnbot.database.filter

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.FilterType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FilterDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "filters"
    override val tableStructure: String =
        "guildId bigint, filterGroupName varchar(32), type varchar(32), filter varchar(2048)"
    override val primaryKey: String = "guildId, filterGroupName, type, filter"

    override val cacheName: String = "filter"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun add(guildId: Long, filterGroupName: String, type: FilterType, filter: String) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guildId, filterGroupName, type, filter) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            guildId, filterGroupName, type.toString(), filter
        )
    }

    fun remove(guildId: Long, filterGroupName: String, type: FilterType, filter: String) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guildId = ? AND filterGroupName = ? AND type = ? AND filter = ?",
            guildId, filterGroupName, type.toString(), filter
        )
    }

    suspend fun get(guildId: Long, filterGroupName: String, type: FilterType): List<String> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE guildId = ? AND filterGroupName = ? AND type = ? ",
            { rs ->
                val filters = mutableListOf<String>()
                while (rs.next()) {
                    filters.add(rs.getString("filter"))
                }
                it.resume(filters)
            },
            guildId,
            filterGroupName,
            type.toString()
        )
    }
}