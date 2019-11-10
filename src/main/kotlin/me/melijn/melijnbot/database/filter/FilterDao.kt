package me.melijn.melijnbot.database.filter

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.FilterType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FilterDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "filters"
    override val tableStructure: String = "guildId bigint, channelId bigint, type varchar(32), filter varchar(2048)"
    override val primaryKey: String = "guildId, channelId, type, filter"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun add(guildId: Long, channelId: Long?, type: FilterType, filter: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelId, type, filter) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            guildId, channelId, type, filter)
    }

    suspend fun get(guildId: Long, channelId: Long?, type: FilterType): List<String> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND channelId = ? AND type = ? ", { rs ->
            val filters = mutableListOf<String>()
            while (rs.next()) {
                filters.add(rs.getString("filter"))
            }
            it.resume(filters)
        }, guildId, channelId, type)
    }

    suspend fun remove(guildId: Long, channelId: Long?, type: FilterType, filter: String) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND channelId = ? AND type = ? AND filter = ?",
            guildId, channelId, type, filter)
    }

}