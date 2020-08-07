package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class JoinRoleGroupDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "joinRoleGroups"
    override val tableStructure: String = "guildId bigint, groupname varchar(64), getAllRoles boolean, isEnabled boolean"
    override val primaryKey: String = "guildId, groupname"

    override val cacheName: String = "joinrole:group"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun put(guildId: Long, joinRoleGroupInfo: JoinRoleGroupInfo) {
        joinRoleGroupInfo.apply {
            driverManager.executeUpdate("INSERT INTO $table (guildId, groupName, getAllRoles, isEnabled) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT ($primaryKey) DO UPDATE SET getAllRoles = ?, isEnabled = ?",
                guildId, groupName, getAllRoles, isEnabled, getAllRoles, isEnabled)
        }
    }

    fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
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
            it.resume(list)
        }, guildId)
    }
}

data class JoinRoleGroupInfo(
    val groupName: String,
    var getAllRoles: Boolean,
    var isEnabled: Boolean
)