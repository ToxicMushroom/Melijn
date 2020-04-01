package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SelfRoleGroupDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "selfRoleGroups"
    override val tableStructure: String = "guildId bigint, groupName varchar(64), mode varchar(16), isSelfRoleable boolean"
    override val primaryKey: String = "guildId, groupName"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(guildId: Long): List<SelfRoleGroup> = suspendCoroutine {
        val list = mutableListOf<SelfRoleGroup>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            while (rs.next()) {
                list.add(
                    SelfRoleGroup(
                        rs.getString("groupName"),
                        SelfRoleGroup.SelfRoleGroupMode.valueOf(rs.getString("mode")),
                        rs.getBoolean("isSelfRoleable")
                    )
                )
            }
            it.resume(list)
        }, guildId)
    }


    suspend fun set(guildId: Long, groupName: String, mode: String, isSelfRoleable: Boolean) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, groupName, mode, isSelfRoleable) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET mode = ? AND isSelfRoleable = ?",
            guildId, groupName, mode, isSelfRoleable, mode, isSelfRoleable)
    }

    suspend fun remove(guildId: Long, groupName: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND groupName = ?",
            guildId, groupName)
    }
}
data class SelfRoleGroup(
    val groupName: String,
    val mode: SelfRoleGroupMode,
    val isSelfRoleable: Boolean
) {

    enum class SelfRoleGroupMode {
        AUTO, MANUAL
    }
}