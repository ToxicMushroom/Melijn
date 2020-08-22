package me.melijn.melijnbot.database.economy

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DailyCooldownDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "dailyCooldowns"
    override val tableStructure: String = "userId bigint, lastTime bigint"
    override val primaryKey: String = "userId"

    override val cacheName: String = "dailycooldown"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(userId: Long, time: Long) {
        driverManager.executeUpdate("INSERT INTO $table (userId, lastTime) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET lastTime=?",
            userId, time, time)

    }

    suspend fun get(userId: Long): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId=?", { rs ->
            if (rs.next()) {
                it.resume(rs.getLong("lastTime"))
            } else {
                it.resume(0)
            }
        }, userId)
    }
}