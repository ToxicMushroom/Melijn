package me.melijn.melijnbot.database.permission

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.PermState
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChannelRolePermissionDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "channelRolePermissions"
    override val tableStructure: String =
        "guildId bigint, channelId bigint, roleId bigint, permission varchar(64), state varchar(8)"
    override val primaryKey: String = "channelId, roleId, permission"

    override val cacheName: String = "permission:channel:role"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun getPermState(channelId: Long, roleId: Long, permission: String, permState: (PermState) -> Unit) {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE roleId = ? AND permission = ? AND channelId = ?",
            { resultset ->
                if (resultset.next()) {
                    permState.invoke(PermState.valueOf(resultset.getString("state")))
                } else permState.invoke(PermState.DEFAULT)
            },
            roleId,
            permission,
            channelId
        )
    }

    fun set(guildId: Long, channelId: Long, roleId: Long, permission: String, permState: PermState) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guildId, channelId, roleId, permission, state) VALUES (?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET state = ?",
            guildId, channelId, roleId, permission, permState.toString(), permState.toString()
        )
    }

    fun delete(channelId: Long, roleId: Long, permission: String) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE channelId = ? AND roleId = ? AND permission = ?",
            channelId, roleId, permission
        )
    }

    fun delete(channelId: Long, roleId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE channelId = ? AND roleId = ?",
            channelId, roleId
        )
    }

    suspend fun getMap(channelId: Long, roleId: Long): Map<String, PermState> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE roleId = ? AND channelId = ?", { rs ->
            val map = HashMap<String, PermState>()
            while (rs.next()) {
                map[rs.getString("permission").lowercase()] = PermState.valueOf(rs.getString("state"))
            }
            it.resume(map)
        }, roleId, channelId)
    }


    fun bulkPut(guildId: Long, channelId: Long, roleId: Long, permissions: List<String>, state: PermState) {
        driverManager.getUsableConnection { connection ->
            connection.prepareStatement("INSERT INTO $table (guildId, channelId, roleId, permission, state) VALUES (?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET state = ?")
                .use { statement ->
                    statement.setLong(1, guildId)
                    statement.setLong(2, channelId)
                    statement.setLong(3, roleId)
                    statement.setString(5, state.toString())
                    statement.setString(6, state.toString())
                    for (perm in permissions) {
                        statement.setString(4, perm)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
        }
    }

    fun bulkDelete(channelId: Long, roleId: Long, permissions: List<String>) {
        driverManager.getUsableConnection { connection ->
            connection.prepareStatement("DELETE FROM $table WHERE channelId = ? AND roleId = ? AND permission = ?")
                .use { statement ->
                    statement.setLong(1, channelId)
                    statement.setLong(2, roleId)
                    for (perm in permissions) {
                        statement.setString(3, perm)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
        }
    }

    fun migrateChannel(oldId: Long, newId: Long) {
        driverManager.executeUpdate("UPDATE $table SET channelId = ? WHERE channelId = ?", newId, oldId)
    }
}