package me.melijn.melijnbot.database.autopunishment

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AutoPunishmentDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "autoPunishments"
    override val tableStructure: String = "guildId bigint, userId bigint, pointsMap varchar(1024)"
    override val primaryKey: String = "guildId, userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(guildId: Long, userId: Long): String = suspendCoroutine{
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND userId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("pointsMap"))
            } else {
                it.resume("")
            }
        }, guildId, userId)
    }

    suspend fun set(guildId: Long, userId: Long, pointsMap: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, userId, pointsMap) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET pointsMap = ?",
            guildId, userId, pointsMap, pointsMap)
    }

}