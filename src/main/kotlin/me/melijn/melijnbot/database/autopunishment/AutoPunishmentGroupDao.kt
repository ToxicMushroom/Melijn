package me.melijn.melijnbot.database.autopunishment

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AutoPunishmentGroupDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "autoPunishmentGroups"
    override val tableStructure: String = "guildId bigint, group varchar(64), enabledTypes varchar(512), pointGoalMap varchar(512)"
    override val primaryKey: String = "guildId, group"

    init {
        //driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun setTypePointsMap(guildId: Long, group: String, enabledTypes: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, group, enabledTypes, pointGoalMap) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET typePointsMap = ?",
            guildId, group, enabledTypes, "", enabledTypes)
    }

    suspend fun setPointGoalMap(guildId: Long, group: String, pointGoalMap: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, group, enabledTypes, pointGoalMap) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET pointGoalMap = ?",
            guildId, group, "", pointGoalMap, pointGoalMap)
    }

    suspend fun get(guildId: Long, group: String): Pair<String, String> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND groupId = ?", { rs ->
            if (rs.next()) {
                it.resume(Pair(rs.getString("typePointsMap"), rs.getString("pointGoalMap")))
            } else {
                it.resume(Pair("", ""))
            }
        }, guildId, group)
    }

    suspend fun add(guildId: Long, group: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, group, typePointsMap, pointGoalMap) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            guildId, group, "", "")
    }

    suspend fun remove(guildId: Long, group: String) {
        driverManager.executeUpdate("DELETED FROM $table WHERE guildId = ? AND group = ?",
            guildId, group)
    }

    suspend fun getAll(guildId: Long): Map<String, Pair<String, String>> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val map = mutableMapOf<String, Pair<String, String>>()
            while (rs.next()) {
                map[rs.getString("group")] = Pair(rs.getString("typePointsMap"), rs.getString("pointGoalMap"))
            }
            it.resume(map)
        }, guildId)
    }
}