package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TempRoleDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "tempRoles"
    override val tableStructure: String =
        "guildId bigint, userId bigint, roleId bigint, start bigint, endTime bigint, added boolean"
    override val primaryKey: String = "userId, roleId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(guildId: Long, userId: Long, roleId: Long, start: Long, end: Long, added: Boolean) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guildId, userId, roleId, start, endTime, added) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET endTime = ?, added = ?",
            guildId, userId, roleId, start, end, added, end, added
        )
    }

    fun remove(userId: Long, roleId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE userId = ? AND roleId = ?",
            userId, roleId
        )
    }

    suspend fun getMap(guildId: Long): Map<String, Long> = suspendCoroutine {
        val map = mutableMapOf<String, Long>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            while (rs.next()) {
                map[rs.getString("emoteji")] = rs.getLong("roleId")
            }
        }, guildId)
        it.resume(map)
    }

    suspend fun getFinishedObjects(endThreshold: Long): List<TempRoleInfo> = suspendCoroutine {
        val list = mutableListOf<TempRoleInfo>()
        driverManager.executeQuery("SELECT * FROM $table WHERE endTime < ?", { rs ->
            while (rs.next()) {
                list.add(
                    TempRoleInfo(
                        rs.getLong("guildId"),
                        rs.getLong("userId"),
                        rs.getLong("roleId"),
                        rs.getLong("start"),
                        rs.getLong("endTime"),
                        rs.getBoolean("added")
                    )
                )
            }
        }, endThreshold)
        it.resume(list)
    }
}

data class TempRoleInfo(
    val guildId: Long,
    val userId: Long,
    val roleId: Long,
    val startTime: Long,
    val endTime: Long,
    val added: Boolean
)