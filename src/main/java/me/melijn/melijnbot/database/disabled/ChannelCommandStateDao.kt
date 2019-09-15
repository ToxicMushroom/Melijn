package me.melijn.melijnbot.database.disabled

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.ChannelCommandState
import me.melijn.melijnbot.objects.command.AbstractCommand

class ChannelCommandStateDao(driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "channelCommandStates"
    override val tableStructure: String = "guildId bigint, channelId bigint, commandId int, state varchar(32)"
    override val keys: String = "UNIQUE KEY (channelId, commandId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(channelId: Long, disabled: (Map<Int, ChannelCommandState>) -> Unit) {
        val map = HashMap<Int, ChannelCommandState>()
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ?", { resultSet ->
            while (resultSet.next()) {
                map[resultSet.getInt("commandId")] = ChannelCommandState.valueOf(resultSet.getString("state"))
            }
        }, channelId)
        disabled.invoke(map)
    }

    fun contains(channelId: Long, commandId: Int, contains: (Boolean) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ? AND commandId = ?", {
            contains.invoke(it.next())
        }, channelId, commandId)
    }

    suspend fun insert(guildId: Long, channelId: Long, commandId: Int, state: ChannelCommandState) {
        driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, channelId, commandId) VALUES (?, ?, ?)",
            guildId, channelId, commandId, state.toString())
    }

    fun bulkPut(guildId: Long, channelId: Long, commands: Set<AbstractCommand>, channelCommandState: ChannelCommandState) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("INSERT INTO $table (guildId, channelId, commandId, state) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE state = ?").use { statement ->
                statement.setLong(1, guildId)
                statement.setLong(2, channelId)
                statement.setString(4, channelCommandState.toString())
                statement.setString(5, channelCommandState.toString())
                for (cmd in commands) {
                    statement.setInt(3, cmd.id)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }

    fun bulkRemove(channelId: Long, commands: Set<AbstractCommand>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("DELETE FROM $table WHERE channelId = ? AND commandId = ?").use { statement ->
                statement.setLong(1, channelId)
                for (cmd in commands) {
                    statement.setInt(2, cmd.id)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }
}