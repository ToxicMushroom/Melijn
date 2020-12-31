package me.melijn.melijnbot.database.birthday

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BirthdayHistoryDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "birthdayHistory"
    override val tableStructure: String = "year int, guildId bigint, userId bigint, start bigint, active boolean"
    override val primaryKey: String = "year, guildId, userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun add(year: Int, guildId: Long, userId: Long, start: Long) {
        driverManager.executeUpdate("INSERT INTO $table (year, guildId, userId, start, active) VALUES (?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            year, guildId, userId, start, true)
    }

    suspend fun getBirthdaysToRemove(yesterdayTime: Long): Map<Long, Pair<Int, Long>> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE active = ? AND start < ?", { rs ->
            val map = mutableMapOf<Long, Pair<Int, Long>>()

            while (rs.next()) {
                map[rs.getLong("userId")] = Pair(rs.getInt("year"), rs.getLong("start"))
            }

            it.resume(map)
        }, true, yesterdayTime)
    }

    suspend fun contains(year: Int, guildId: Long, userId: Long): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE year = ? AND guildId = ? AND userId = ?", { rs ->
            it.resume(rs.next())
        }, year, guildId, userId)
    }

    suspend fun isActive(year: Int, guildId: Long, userId: Long): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE year = ? AND guildId = ? AND userId = ?", { rs ->
            it.resume(rs.next())
        }, year, guildId, userId)
    }

    suspend fun deactivate(year: Int, guildId: Long, userId: Long) {
        driverManager.executeUpdate("UPDATE $table SET active = ? WHERE year = ? AND guildId = ? AND userId = ?",
            false, year, guildId, userId)
    }
}