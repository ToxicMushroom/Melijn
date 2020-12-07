package me.melijn.melijnbot.database.economy

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EconomyCooldownDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "economyCooldowns"
    override val tableStructure: String = "userId bigint, key varchar(32), lastTime bigint"
    override val primaryKey: String = "userId, key"

    override val cacheName: String = "economyCooldown"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(userId: Long, key: String, time: Long) {
        driverManager.executeUpdate(
            "INSERT INTO $table (userId, key, lastTime) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET lastTime = ?",
            userId, key, time, time
        )

    }

    suspend fun get(userId: Long, key: String): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ? AND key = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getLong("lastTime"))
            } else {
                it.resume(0)
            }
        }, userId, key)
    }
}