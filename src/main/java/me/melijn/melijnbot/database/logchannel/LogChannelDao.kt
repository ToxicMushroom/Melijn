package me.melijn.melijnbot.database.logchannel

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.LogChannelType

class LogChannelDao(val driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "logChannels"
    override val tableStructure: String = "guildId bigInt, type varchar(64), channelId bigInt"
    override val keys: String = "UNIQUE KEY (guildId, type)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(guildId: Long, type: LogChannelType, channelId: (Long) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND type = ?", { resultSet ->
            if (resultSet.next()) {
                channelId.invoke(resultSet.getLong("channelId"))
            } else {
                channelId.invoke(-1)
            }

        }, guildId, type.toString())
    }

    fun set(guildId: Long, type: LogChannelType, channelId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, type, channelId) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE channelId = ?",
                guildId, type.toString(), channelId, channelId)
    }

    fun unset(guildId: Long, type: LogChannelType) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND type = ?",
                guildId, type.toString())
    }


    fun bulkPut(guildId: Long, logChannelTypes: List<LogChannelType>, channelId: Long) {
        driverManager.getUsableConnection {con ->
            con.prepareStatement("INSERT INTO $table (guildId, type, channelId) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE channelId = ?").use {
                statement ->
                statement.setLong(1, guildId)
                statement.setLong(3, channelId)
                statement.setLong(4, channelId)
                for (type in logChannelTypes) {
                    statement.setString(2, type.toString())
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }

        }
    }

    fun bulkRemove(guildId: Long, logChannelTypes: List<LogChannelType>) {
        driverManager.getUsableConnection {con ->
            con.prepareStatement("REMOVE FROM $table WHERE guildId = ? AND type = ?").use {
                statement ->
                statement.setLong(1, guildId)
                for (type in logChannelTypes) {
                    statement.setString(2, type.toString())
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }
}