package me.melijn.melijnbot.database.games

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OsuDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "osu"
    override val tableStructure: String = "userId bigint, name varchar(32)"
    override val primaryKey: String = "userId"

    override val cacheName: String = "osu"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(userId: Long, name: String) {
        driverManager.executeUpdate("INSERT INTO $table (userId, name) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET name = ?",
            userId, name, name)
    }

    suspend fun get(userId: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { rs ->
            if (rs.next()) it.resume(rs.getString("name"))
            else it.resume("")
        }, userId)
    }

    fun remove(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ?", userId)
    }
}