package me.melijn.melijnbot.database.autopunishment

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AutoPunishmentGroupDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "autoPunishmentGroups"
    override val tableStructure: String = "groupId bigint, guildId bigint, typePointsMap varchar(512)"
    override val primaryKey: String = "groupId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun set(guildId: Long, groupId: Long, typePointsMap: String) {
        driverManager.executeUpdate("INSERT INTO $table (groupId, guildId, types) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET typePointsMap = ?",
            groupId, guildId, typePointsMap, typePointsMap)
    }

    suspend fun get(guildId: Long, groupId: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE groupId = ? AND guildId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("typePointsMap"))
            } else {
                it.resume("")
            }
        }, groupId, guildId)
    }
}