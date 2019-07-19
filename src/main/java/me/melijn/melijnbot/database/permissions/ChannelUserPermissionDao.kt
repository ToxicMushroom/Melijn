package me.melijn.melijnbot.database.permissions

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.PermState
import java.util.function.Consumer

class ChannelUserPermissionDao(private val driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "channelUserPermissions"
    override val tableStructure: String = "guildId bigint, channelId bigint, userId bigint, permission varchar(64), state varchar(8)"
    override val keys: String = "UNIQUE KEY (userId, channelId, permission)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun getPermState(channelId: Long, userId: Long, permission: String, permState: Consumer<PermState>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ? AND permission = ? AND channelId = ?", Consumer { resultset ->
            if (resultset.next()) {
                permState.accept(PermState.valueOf(resultset.getString("state")))
            } else permState.accept(PermState.DEFAULT)
        }, userId, permission)
    }

    fun set(guildId: Long, channelId: Long, userId: Long, permission: String, permState: PermState) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelId, userId, permission, state) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE state = ?",
                guildId, channelId, userId, permission, permState.toString(), permState.toString())
    }

    fun getMap(channelId: Long, userId: Long, permStateMap: Consumer<Map<String, PermState>>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ? AND channelId = ?", Consumer { resultset ->
            val map = HashMap<String, PermState>()
            while (resultset.next()) {
                map[resultset.getString("permission")] = PermState.valueOf(resultset.getString("state"))
            }
            permStateMap.accept(map)
        }, userId, channelId)
    }
}