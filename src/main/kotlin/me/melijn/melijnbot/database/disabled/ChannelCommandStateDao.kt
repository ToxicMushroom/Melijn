package me.melijn.melijnbot.database.disabled

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.models.TriState
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChannelCommandStateDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "channelCommandStates"
    override val tableStructure: String = "guildId bigint, channelId bigint, commandId varchar(16), state varchar(32)"
    override val primaryKey: String = "channelId, commandId"

    override val cacheName: String = "channel:command:state"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(channelId: Long): Map<String, TriState> = suspendCoroutine {
        val map = HashMap<String, TriState>()
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ?", { resultSet ->
            while (resultSet.next()) {
                map[resultSet.getString("commandId")] = TriState.valueOf(resultSet.getString("state"))
            }
        }, channelId)
        it.resume(map)
    }

    suspend fun contains(channelId: Long, commandId: Int): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ? AND commandId = ?", { rs ->
            it.resume(rs.next())
        }, channelId, commandId)
    }

    fun insert(guildId: Long, channelId: Long, commandId: String, state: TriState) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guildId, channelId, commandId) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            guildId, channelId, commandId, state.toString()
        )
    }

    fun bulkPut(guildId: Long, channelId: Long, commands: Set<String>, channelCommandState: TriState) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("INSERT INTO $table (guildId, channelId, commandId, state) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET state = ?")
                .use { statement ->
                    statement.setLong(1, guildId)
                    statement.setLong(2, channelId)
                    statement.setString(4, channelCommandState.toString())
                    statement.setString(5, channelCommandState.toString())
                    for (id in commands) {
                        statement.setString(3, id)
                        statement.addBatch()
                    }
                    statement.executeBatch()
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
                statement.executeBatch()
            }
        }
    }

    fun migrateChannel(oldId: Long, newId: Long) {
        driverManager.executeUpdate("UPDATE $table SET channelId = ? WHERE channelId = ?", newId, oldId)
    }
}