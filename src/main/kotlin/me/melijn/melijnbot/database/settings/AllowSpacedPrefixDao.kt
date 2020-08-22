package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AllowSpacedPrefixDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "allowSpacedPrefixStates"
    override val tableStructure: String = "guildId bigint"
    override val primaryKey: String = "guildId"

    override val cacheName: String = "allowspacedprefix"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun add(id: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId) VALUES (?) ON CONFLICT ($primaryKey) DO NOTHING",
            id)
    }

    fun delete(id: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", id)
    }

    suspend fun contains(id: Long): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            it.resume(rs.next())
        }, id)
    }
}