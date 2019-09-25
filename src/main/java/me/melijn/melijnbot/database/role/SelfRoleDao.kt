package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SelfRoleDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "selfRoles"
    override val tableStructure: String = "guildId bigint, roleId bigint, emoteId bigint"
    override val keys: String = "PRIMARY KEY roleId"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun set(guildId: Long, roleId: Long, emoteId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, roleId, emoteId) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE emoteId = ?",
            guildId, roleId, emoteId, emoteId)
    }

    suspend fun getMap(guildId: Long): Map<Long, Long> = suspendCoroutine {
        val map = mutableMapOf<Long, Long>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            while (rs.next()) {
                map[rs.getLong("roleId")] = rs.getLong("emoteId")
            }
        }, guildId)
        it.resume(map)
    }

    suspend fun remove(guildId: Long, roleId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND roleId = ?",
            guildId, roleId)
    }


}