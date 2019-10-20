package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.RoleType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RoleDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "roles"
    override val tableStructure: String = "guildId bigInt UNIQUE, roleType varchar(32) UNIQUE, roleId bigint"
    override val keys: String = ""

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun set(guildId: Long, roleType: RoleType, roleId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, roleType, roleId) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE roleId = ?",
            guildId, roleType.toString(), roleId, roleId)
    }

    suspend fun get(guildId: Long, roleType: RoleType): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND roleType = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getLong("roleId"))
            } else {
                it.resume(-1)
            }
        }, guildId, roleType.toString())
    }

    suspend fun unset(guildId: Long, roleType: RoleType) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND roleType = ?", guildId, roleType.toString())
    }
}