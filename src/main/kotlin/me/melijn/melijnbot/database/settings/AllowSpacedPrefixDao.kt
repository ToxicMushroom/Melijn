package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AllowSpacedPrefixDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "allowSpacedPrefixStates"
    override val tableStructure: String = "guildId bigint"
    override val primaryKey: String = "guildId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun add(id: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId) VALUES (?) ON CONFLICT ($primaryKey) DO NOTHING",
            id)
    }

    suspend fun contains(id: Long): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            it.resume(rs.next())
        }, id)
    }

    suspend fun delete(id: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", id)
    }
}