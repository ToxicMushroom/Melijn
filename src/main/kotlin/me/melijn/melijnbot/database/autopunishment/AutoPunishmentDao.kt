package me.melijn.melijnbot.database.autopunishment

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AutoPunishmentDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "autoPunishments"
    override val tableStructure: String = "guildId bigint, userId bigint, pointsMap varchar(1024)"
    override val primaryKey: String = "guildId, userId"

    override val cacheName: String = "autopunishment"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(guildId: Long, userId: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND userId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("pointsMap"))
            } else {
                it.resume("")
            }
        }, guildId, userId)
    }

    fun set(guildId: Long, userId: Long, pointsMap: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, userId, pointsMap) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET pointsMap = ?",
            guildId, userId, pointsMap, pointsMap)
    }
}