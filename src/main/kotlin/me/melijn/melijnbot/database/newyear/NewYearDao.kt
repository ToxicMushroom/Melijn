package me.melijn.melijnbot.database.newyear

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NewYearDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val cacheName: String = "newyearmsg"

    override val table: String = "newYearMessages"
    override val tableStructure: String = "year int, userId bigint"
    override val primaryKey: String = "year, userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun add(year: Int, userId: Long) {
        driverManager.executeUpdate(
            "INSERT INTO $table (year, userId) VALUES (?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            year, userId
        )
    }

    suspend fun contains(year: Int, userId: Long): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE year = ? AND userId = ?", { rs ->
            it.resume(rs.next())
        }, year, userId)
    }
}