package me.melijn.melijnbot.database.filter

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.FilterType

class FilterDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "filters"
    override val tableStructure: String = "guildId bigint, type varchar(32), filter varchar(2048)"
    override val primaryKey: String = "guildId, type, filter"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun add(guildId: Long, type: FilterType, filter: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, type, filter) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            guildId, type, filter)
    }
}