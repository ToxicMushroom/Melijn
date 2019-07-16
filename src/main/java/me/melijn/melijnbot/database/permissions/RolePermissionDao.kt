package me.melijn.melijnbot.database.permissions

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.PermState
import java.util.function.Consumer

class RolePermissionDao(private val driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "rolePermissions"
    override val tableStructure: String = "guildId bigint, roleId bigint, permission varchar(64), state varchar(8)"
    override val keys: String = "UNIQUE KEY (roleId, permission)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(roleId: Long, permState: Consumer<PermState>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE roleId = ? AND permission = ?", Consumer { resultset ->
            if (resultset.next()) {
                permState.accept(PermState.valueOf(resultset.getString("state")))
            } else permState.accept(PermState.DEFAULT)
        })
    }

    fun set(guildId: Long, roleId: Long, permission: String, permState: PermState) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, roleId, permission, state) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE state = ?",
                guildId, roleId, permission, permState.toString(), permState.toString())
    }
}