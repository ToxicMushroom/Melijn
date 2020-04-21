package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.suspendCoroutine

class JoinRoleGroupDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "joinRoleGroups"
    override val tableStructure: String = "guildId bigint, groupname varchar(64), getAllRoles boolean, isEnabled boolean"
    override val primaryKey: String = "guildId, groupname"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun put(guildId: Long, joinRoleGroupInfo: JoinRoleGroupInfo) {
        joinRoleGroupInfo.apply {
            driverManager.executeUpdate("INSERT INTO $table (guildId, groupName, getAllRoles, isEnabled) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT ($primaryKey) DO UPDATE SET getAllRoles = ?, isEnabled = ?",
                guildId, groupName, getAllRoles, isEnabled, getAllRoles, isEnabled)
        }
    }

    suspend fun remove(guildId: Long) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ?", guildId)
    }

    suspend fun get(guildId: Long): List<JoinRoleGroupInfo> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val list = mutableListOf<JoinRoleGroupInfo>()
            while (rs.next()) {
                list.add(JoinRoleGroupInfo(
                    rs.getString("groupName"),
                    rs.getBoolean("getAllRoles"),
                    rs.getBoolean("isEnabled")
                ))
            }
        }, guildId)
    }
}

data class JoinRoleGroupInfo(
    val groupName: String,
    val getAllRoles: Boolean,
    val isEnabled: Boolean
)