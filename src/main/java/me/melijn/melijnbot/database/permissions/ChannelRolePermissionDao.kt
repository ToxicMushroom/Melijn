package me.melijn.melijnbot.database.permissions

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.PermState
import java.util.function.Consumer

class ChannelRolePermissionDao(private val driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "channelRolePermissions"
    override val tableStructure: String = "guildId bigint, channelId bigint, roleId bigint, permission varchar(64), state varchar(8)"
    override val keys: String = "UNIQUE KEY (channelId, roleId, permission)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun getPermState(channelId: Long, roleId: Long, permission: String, permState: Consumer<PermState>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE roleId = ? AND permission = ? AND channelId = ?", Consumer { resultset ->
            if (resultset.next()) {
                permState.accept(PermState.valueOf(resultset.getString("state")))
            } else permState.accept(PermState.DEFAULT)
        }, roleId, permission, channelId)
    }

    fun set(guildId: Long, channelId: Long, roleId: Long, permission: String, permState: PermState) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelId, roleId, permission, state) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE state = ?",
                guildId, channelId, roleId, permission, permState.toString(), permState.toString())
    }

    fun delete(channelId: Long, roleId: Long, permission: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE channelId = ? AND roleId = ? AND permission = ?",
                channelId, roleId, permission)
    }

    fun delete(channelId: Long, roleId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE channelId = ? AND roleId = ?",
                channelId, roleId)
    }

    fun getMap(channelId: Long, roleId: Long, permStateMap: Consumer<Map<String, PermState>>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE roleId = ? AND channelId = ?", Consumer { resultset ->
            val map = HashMap<String, PermState>()
            while (resultset.next()) {
                map[resultset.getString("permission")] = PermState.valueOf(resultset.getString("state"))
            }
            permStateMap.accept(map)
        }, roleId, channelId)
    }


    fun bulkPut(guildId: Long, channelId: Long, roleId: Long, permissions: List<String>, state: PermState) {
        driverManager.getUsableConnection(Consumer { connection ->
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
        })
    }

    fun bulkDelete(channelId: Long, roleId: Long, permissions: List<String>) {
        driverManager.getUsableConnection(Consumer { connection ->
            connection.prepareStatement("DELETE FROM $table WHERE channelId = ? AND roleId = ? AND permission = ?").use { statement ->
                statement.setLong(1, channelId)
                statement.setLong(2, roleId)
                for (perm in permissions) {
                    statement.setString(3, perm)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        })
    }
}