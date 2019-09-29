package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ForceRoleDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "forcedRoles"
    override val tableStructure: String = "guildId bigint, userId bigint, roleId bigint"
    override val keys: String = "PRIMARY KEY(roleId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun getMap(guildId: Long): Map<Long, List<Long>> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val map = mutableMapOf<Long, List<Long>>()
            val list = mutableListOf<Long>()
            while (rs.next()) {
                list.add(rs.getLong("roleId"))
            }
            map[rs.getLong("userId")] = list
            it.resume(map)
        }, guildId)
    }

    suspend fun add(guildId: Long, userId: Long, roleId: Long) {
        driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, userId, roleId) VALUES (?, ?, ?)",
            guildId, userId, roleId)
    }

    suspend fun remove(guildId: Long, userId: Long, roleId: Long) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND userId = ? AND roleId = ?",
            guildId, userId, roleId)
    }

}