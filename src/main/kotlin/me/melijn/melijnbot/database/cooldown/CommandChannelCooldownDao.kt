package me.melijn.melijnbot.database.cooldown

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CommandChannelCooldownDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "commandChannelCooldowns"
    override val tableStructure: String = "guildId bigint, channelId bigint, commandId varchar(16), cooldownMillis bigint"
    override val primaryKey: String = "guildId, commandId"

    override val cacheName: String = "command:channel:cooldown"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun getCooldownMapForChannel(channelId: Long): Map<String, Long> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ?", {rs ->
            val map = HashMap<String, Long>()
            while (rs.next()) {
                map[rs.getString("commandId")] = rs.getLong("cooldownMillis")
            }
            it.resume(map)
        }, channelId)
    }

    fun bulkPut(guildId: Long, channelId: Long, commandsIds: Set<String>, cooldownMillis: Long) {
        val sql = "INSERT INTO $table (guildId, channelId, commandId, cooldownMillis) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET cooldownMillis = ?"
        driverManager.getUsableConnection { con ->
            con.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setLong(1, guildId)
                preparedStatement.setLong(2, channelId)
                preparedStatement.setLong(4, cooldownMillis)
                preparedStatement.setLong(5, cooldownMillis)
                for (cmdId in commandsIds) {
                    preparedStatement.setString(3, cmdId)
                    preparedStatement.addBatch()
                }
                preparedStatement.executeBatch()
            }
        }
    }

    fun bulkDelete(channelId: Long, commandsIds: Set<String>) {
        val sql = "DELETE FROM $table WHERE channelId = ? AND commandId = ?"
        driverManager.getUsableConnection { con ->
            con.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.setLong(1, channelId)
                for (cmdId in commandsIds) {
                    preparedStatement.setString(2, cmdId)
                    preparedStatement.addBatch()
                }
                preparedStatement.executeBatch()
            }
        }
    }
}