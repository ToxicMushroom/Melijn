package me.melijn.melijnbot.database.permission

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.PermState
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChannelRolePermissionDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "channelRolePermissions"
    override val tableStructure: String = "guildId bigint, channelId bigint UNIQUE, roleId bigint UNIQUE, permission varchar(64) UNIQUE, state varchar(8)"
    override val keys: String = ""

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun getPermState(channelId: Long, roleId: Long, permission: String, permState: (PermState) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE roleId = ? AND permission = ? AND channelId = ?", { resultset ->
            if (resultset.next()) {
                permState.invoke(PermState.valueOf(resultset.getString("state")))
            } else permState.invoke(PermState.DEFAULT)
        }, roleId, permission, channelId)
    }

    suspend fun set(guildId: Long, channelId: Long, roleId: Long, permission: String, permState: PermState) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelId, roleId, permission, state) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE state = ?",
            guildId, channelId, roleId, permission, permState.toString(), permState.toString())
    }

    suspend fun delete(channelId: Long, roleId: Long, permission: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE channelId = ? AND roleId = ? AND permission = ?",
            channelId, roleId, permission)
    }

    suspend fun delete(channelId: Long, roleId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE channelId = ? AND roleId = ?",
            channelId, roleId)
    }

    suspend fun getMap(channelId: Long, roleId: Long): Map<String, PermState> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE roleId = ? AND channelId = ?", { resultset ->
            val map = HashMap<String, PermState>()
            while (resultset.next()) {
                map[resultset.getString("permission")] = PermState.valueOf(resultset.getString("state"))
            }
            it.resume(map)
        }, roleId, channelId)
    }


    fun bulkPut(guildId: Long, channelId: Long, roleId: Long, permissions: List<String>, state: PermState) {
        driverManager.getUsableConnection { connection ->
            connection.prepareStatement("INSERT INTO $table (guildId, channelId, roleId, permission, state) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE state = ?").use { statement ->
                statement.setLong(1, guildId)
                statement.setLong(2, channelId)
                statement.setLong(3, roleId)
                statement.setString(5, state.toString())
                statement.setString(6, state.toString())
                for (perm in permissions) {
                    statement.setString(4, perm)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }

    fun bulkDelete(channelId: Long, roleId: Long, permissions: List<String>) {
        driverManager.getUsableConnection { connection ->
            connection.prepareStatement("DELETE FROM $table WHERE channelId = ? AND roleId = ? AND permission = ?").use { statement ->
                statement.setLong(1, channelId)
                statement.setLong(2, roleId)
                for (perm in permissions) {
                    statement.setString(3, perm)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }
}