package me.melijn.melijnbot.database.logchannel

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.LogChannelType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LogChannelDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "logChannels"
    override val tableStructure: String = "guildId bigInt, type varchar(64), channelId bigInt"
    override val keys: String = "UNIQUE (guildId, type)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun get(guildId: Long, type: LogChannelType): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND type = ?", { resultSet ->
            if (resultSet.next()) {
                it.resume(resultSet.getLong("channelId"))
            } else {
                it.resume(-1)
            }

        }, guildId, type.toString())
    }

    suspend fun set(guildId: Long, type: LogChannelType, channelId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, type, channelId) VALUES (?, ?, ?) ON CONFLICT (guildId,  type) DO UPDATE SET channelId = ?",
            guildId, type.toString(), channelId, channelId)
    }

    suspend fun unset(guildId: Long, type: LogChannelType) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND type = ?",
            guildId, type.toString())
    }


    fun bulkPut(guildId: Long, logChannelTypes: List<LogChannelType>, channelId: Long) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("INSERT INTO $table (guildId, type, channelId) VALUES (?, ?, ?) ON CONFLICT (guildId, type) DO UPDATE SET channelId = ?").use { statement ->
                statement.setLong(1, guildId)
                statement.setLong(3, channelId)
                statement.setLong(4, channelId)
                for (type in logChannelTypes) {
                    statement.setString(2, type.toString())
                    statement.addBatch()
                }
                statement.executeBatch()
            }

        }
    }

    fun bulkRemove(guildId: Long, logChannelTypes: List<LogChannelType>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("REMOVE FROM $table WHERE guildId = ? AND type = ?").use { statement ->
                statement.setLong(1, guildId)
                for (type in logChannelTypes) {
                    statement.setString(2, type.toString())
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }
}