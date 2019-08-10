package me.melijn.melijnbot.database.logchannels

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.LogChannelType
import java.util.function.Consumer

class LogChannelDao(val driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "logChannels"
    override val tableStructure: String = "guildId bigInt, type varchar(64), channelId bigInt"
    override val keys: String = "UNIQUE KEY (guildId, type)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(guildId: Long, type: LogChannelType, func: (Long) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND type = ?", Consumer { resultSet ->
            if (resultSet.next()) {
                func.invoke(resultSet.getLong("channelId"))
            } else {
                func.invoke(-1)
            }

        }, guildId, type.toString())
    }

    fun set(guildId: Long, type: LogChannelType, channelId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, type, channelId) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE channelId = ?",
                guildId, type.toString(), channelId, channelId)
    }


    fun unset(guildId: Long, type: LogChannelType) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND type = ?", guildId, type.toString())
    }
}