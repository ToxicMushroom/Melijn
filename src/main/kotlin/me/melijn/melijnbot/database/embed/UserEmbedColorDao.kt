package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserEmbedColorDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "userEmbedColors"
    override val tableStructure: String = "userId bigint, color int"
    override val primaryKey: String = "userId"

    override val cacheName: String = "user:embed:color"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(userId: Long): Int = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { rs ->
            if (rs.next()) it.resume(rs.getInt("color"))
            else it.resume(0)
        }, userId)
    }

    fun set(userId: Long, color: Int) {
        driverManager.executeUpdate("INSERT INTO $table (userId, color) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET color = ?",
            userId, color, color)
    }

    fun remove(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ?",
            userId)
    }
}