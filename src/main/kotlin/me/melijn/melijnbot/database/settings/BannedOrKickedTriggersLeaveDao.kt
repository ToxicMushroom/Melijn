package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BannedOrKickedTriggersLeaveDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "bannedOrKickedTriggersLeaveStates"
    override val tableStructure: String = "guildId bigint"
    override val primaryKey: String = "guildId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun add(guildId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId) VALUES (?) ON CONFLICT ($primaryKey) DO NOTHING", guildId)
    }

    suspend fun contains(guildId: Long): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            it.resume(rs.next())
        }, guildId)
    }

    suspend fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }

}