package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SelfRoleDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "selfRoles"
    override val tableStructure: String = "guildId bigint, emoteId bigint, roleId bigint"
    override val keys: String = "PRIMARY KEY(emoteId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun set(guildId: Long, emoteId: Long, roleId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, emoteId, roleId) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE roleId = ?",
            guildId, emoteId, roleId, roleId)
    }

    suspend fun getMap(guildId: Long): Map<Long, Long> = suspendCoroutine {
        val map = mutableMapOf<Long, Long>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            while (rs.next()) {
                map[rs.getLong("emoteId")] = rs.getLong("roleId")
            }
        }, guildId)
        it.resume(map)
    }

    suspend fun remove(guildId: Long, emoteId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND emoteId = ?",
            guildId, emoteId)
    }


}