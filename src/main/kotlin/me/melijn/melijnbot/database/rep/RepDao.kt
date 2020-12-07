package me.melijn.melijnbot.database.rep

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RepDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "rep"
    override val tableStructure: String = "userId bigint, rep int"
    override val primaryKey: String = "userId"

    override val cacheName: String = "rep"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(userId: Long, rep: Int) {
        driverManager.executeUpdate(
            "INSERT INTO $table (userId, rep) VALUES (?, ?)",
            userId, rep
        )
    }

    suspend fun get(userId: Long): Int = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM  $table WHERE userId = ?", { rs ->
                if (rs.next()) {
                    it.resume(rs.getInt("rep"))
                } else {
                    it.resume(0)
                }
            },
            userId
        )
    }
}