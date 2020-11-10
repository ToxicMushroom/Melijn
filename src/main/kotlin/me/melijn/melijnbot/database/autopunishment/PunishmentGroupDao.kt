package me.melijn.melijnbot.database.autopunishment

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PunishmentGroupDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "punishmentGroups"
    override val tableStructure: String = "guildId bigint, punishGroup varchar(64), enabledTypes varchar(512), pointGoalMap varchar(512)"
    override val primaryKey: String = "guildId, punishGroup"

    override val cacheName: String = "punishmentgroup"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun setEnabledTypes(guildId: Long, punishGroup: String, enabledTypes: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, punishGroup, enabledTypes, pointGoalMap) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET enabledTypes = ?",
            guildId, punishGroup, enabledTypes, "", enabledTypes)
    }

    fun setPointGoalMap(guildId: Long, punishGroup: String, pointGoalMap: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, punishGroup, enabledTypes, pointGoalMap) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET pointGoalMap = ?",
            guildId, punishGroup, "", pointGoalMap, pointGoalMap)
    }

    suspend fun get(guildId: Long, punishGroup: String): Pair<String, String> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND punishGroup = ?", { rs ->
            if (rs.next()) {
                it.resume(Pair(rs.getString("typePointsMap"), rs.getString("pointGoalMap")))
            } else {
                it.resume(Pair("", ""))
            }
        }, guildId, punishGroup)
    }

    fun add(guildId: Long, punishGroup: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, punishGroup, enabledTypes, pointGoalMap) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            guildId, punishGroup, "", "")
    }

    fun remove(guildId: Long, punishGroup: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND punishGroup = ?",
            guildId, punishGroup)
    }

    suspend fun getAll(guildId: Long): Map<String, Pair<String, String>> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val map = mutableMapOf<String, Pair<String, String>>()
            while (rs.next()) {
                map[rs.getString("punishGroup")] = Pair(rs.getString("enabledTypes"), rs.getString("pointGoalMap"))
            }
            it.resume(map)
        }, guildId)
    }
}