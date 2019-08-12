package me.melijn.melijnbot.database.cooldown

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.objects.command.AbstractCommand
import java.util.function.Consumer

class CommandChannelCooldownDao(val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "commandChannelCooldowns"
    override val tableStructure: String = "guildId bigint, channelId bigint, commandId int, cooldownMillis long"
    override val keys: String = "UNIQUE KEY (channelId, commandId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun getCooldownMapForChannel(channelId: Long, cooldownMap: (Map<Int, Long>) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ?", {
            val map = HashMap<Int, Long>()
            while (it.next()) {
                map[it.getInt("commandId")] = it.getLong("cooldownMillis")
            }
            cooldownMap.invoke(map)
        }, channelId)
    }

    fun bulkPut(guildId: Long, channelId: Long, commands: Set<AbstractCommand>, cooldownMillis: Long) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("INSERT INTO $table (guildId, channelId, commandId, cooldownMillis) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE cooldownMillis = ?").use {
                preparedStatement ->
                preparedStatement.setLong(1, guildId)
                preparedStatement.setLong(2, channelId)
                preparedStatement.setLong(4, cooldownMillis)
                preparedStatement.setLong(5, cooldownMillis)
                for (cmd in commands) {
                    preparedStatement.setInt(3, cmd.id)
                    preparedStatement.addBatch()
                }
                preparedStatement.executeLargeBatch()
            }
        })
    }

    fun bulkDelete(channelId: Long, commands: Set<AbstractCommand>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("DELETE FROM $table WHERE channelId = ? AND commandId = ?").use {
                preparedStatement ->
                preparedStatement.setLong(1, channelId)
                for (cmd in commands) {
                    preparedStatement.setInt(2, cmd.id)
                    preparedStatement.addBatch()
                }
                preparedStatement.executeLargeBatch()
            }
        }
    }
}