package me.melijn.melijnbot.database.cooldown

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GlobalCooldownDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "globalCooldowns"
    override val tableStructure: String = "userId bigint, commandId varchar(64), lastExecuted bigint"
    override val primaryKey: String = "userId, commandId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    override val cacheName: String = "globalcooldowns"

    fun insert(userId: Long, commandId: String, lastExecuted: Long) {
        driverManager.executeUpdate(
            "INSERT INTO $table (userId, commandId, lastExecuted) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET lastExecuted = ?",
            userId, commandId, lastExecuted, lastExecuted
        )
    }

    suspend fun getLastExecuted(userId: Long, commandId: String): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ? AND commandId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getLong("lastExecuted"))
            } else {
                it.resume(0)
            }
        }, userId, commandId)
    }

    fun clearOldExecuted(commandId: String, validTime: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE commandId = ? AND lastExecuted < ?",
            commandId, validTime)
    }
}
