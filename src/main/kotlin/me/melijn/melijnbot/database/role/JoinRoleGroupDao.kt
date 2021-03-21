package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class JoinRoleGroupDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "joinRoleGroups"
    override val tableStructure: String =
        "guildId bigint, groupname varchar(64), getAllRoles boolean, forUserTypes smallint, isEnabled boolean"
    override val primaryKey: String = "guildId, groupname"

    override val cacheName: String = "joinrole:group"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun put(guildId: Long, joinRoleGroupInfo: JoinRoleGroupInfo) {
        joinRoleGroupInfo.apply {
            driverManager.executeUpdate(
                "INSERT INTO $table (guildId, groupName, getAllRoles, forUserTypes, isEnabled) VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT ($primaryKey) DO UPDATE SET getAllRoles = ?, forUserTypes = ?, isEnabled = ?",
                guildId, groupName, getAllRoles, forUserTypes, isEnabled, getAllRoles, forUserTypes, isEnabled
            )
        }
    }

    fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }

    suspend fun get(guildId: Long): List<JoinRoleGroupInfo> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val list = mutableListOf<JoinRoleGroupInfo>()
            while (rs.next()) {
                list.add(
                    JoinRoleGroupInfo(
                        rs.getString("groupName"),
                        rs.getBoolean("getAllRoles"),
                        rs.getInt("forUserTypes"),
                        rs.getBoolean("isEnabled")
                    )
                )
            }
            it.resume(list)
        }, guildId)
    }
}

data class JoinRoleGroupInfo(
    val groupName: String,
    var getAllRoles: Boolean,
    var forUserTypes: Int,
    var isEnabled: Boolean
)

enum class UserType(val id: Int) {
    USER(0),
    BOT(1);

    companion object {
        fun intFromSet(set: Set<UserType>): Int {
            var ret = 0
            for (type in set) {
                ret = ret or (1 shl type.id)
            }
            return ret
        }

        fun setFromInt(bitFlag: Int): Set<UserType> {
            val set = mutableSetOf<UserType>()
            for (type in values()) {
                if ((bitFlag and (1 shl type.id)) != 0)
                set.add(type)
            }
            return set
        }
    }
}