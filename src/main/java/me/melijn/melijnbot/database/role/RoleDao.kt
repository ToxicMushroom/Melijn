package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.RoleType

class RoleDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "roles"
    override val tableStructure: String = "guildId bigInt, roleType varchar(32), roleId bigint"
    override val keys: String = "UNIQUE KEY(guildId, roleType)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun set(guildId: Long, roleType: RoleType, roleId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, roleType, roleId) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE roleId = ?",
            guildId, roleType.toString(), roleId, roleId)
    }

    fun get(guildId: Long, roleType: RoleType, roleId: (Long) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND roleType = ?", { rs ->
            if (rs.next()) {
                roleId.invoke(rs.getLong("roleId"))
            }
        }, guildId, roleType.toString())
    }

    suspend fun unset(guildId: Long, roleType: RoleType) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND roleType = ?", guildId, roleType.toString())
    }
}