package me.melijn.melijnbot.database.disabled

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.ChannelCommandState
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChannelCommandStateDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "channelCommandStates"
    override val tableStructure: String = "guildId bigint, channelId bigint UNIQUE, commandId varchar(16) UNIQUE, state varchar(32)"
    override val keys: String = ""

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun get(channelId: Long): Map<String, ChannelCommandState> = suspendCoroutine {
        val map = HashMap<String, ChannelCommandState>()
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ?", { resultSet ->
            while (resultSet.next()) {
                map[resultSet.getString("commandId")] = ChannelCommandState.valueOf(resultSet.getString("state"))
            }
        }, channelId)
        it.resume(map)
    }

    suspend fun contains(channelId: Long, commandId: Int): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ? AND commandId = ?", { rs ->
            it.resume(rs.next())
        }, channelId, commandId)
    }

    suspend fun insert(guildId: Long, channelId: Long, commandId: String, state: ChannelCommandState) {
        driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, channelId, commandId) VALUES (?, ?, ?)",
            guildId, channelId, commandId, state.toString())
    }

    fun bulkPut(guildId: Long, channelId: Long, commands: Set<String>, channelCommandState: ChannelCommandState) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("INSERT INTO $table (guildId, channelId, commandId, state) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE state = ?").use { statement ->
                statement.setLong(1, guildId)
                statement.setLong(2, channelId)
                statement.setString(4, channelCommandState.toString())
                statement.setString(5, channelCommandState.toString())
                for (id in commands) {
                    statement.setString(3, id)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }

    fun bulkRemove(channelId: Long, commands: Set<String>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("DELETE FROM $table WHERE channelId = ? AND commandId = ?").use { statement ->
                statement.setLong(1, channelId)
                for (id in commands) {
                    statement.setString(2, id)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }
}