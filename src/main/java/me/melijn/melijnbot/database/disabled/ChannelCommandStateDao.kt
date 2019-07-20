package me.melijn.melijnbot.database.disabled

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.ChannelCommandState
import java.util.function.Consumer

class ChannelCommandStateDao(val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "channelCommandStates"
    override val tableStructure: String = "guildId bigint, channelId bigint, commandId int, state varchar(32)"
    override val keys: String = "UNIQUE KEY (channelId, commandId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(channelId: Long, disabled: Consumer<Map<Int, ChannelCommandState>>) {
        val map = HashMap<Int, ChannelCommandState>()
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ?", Consumer { resultSet ->
            while (resultSet.next()) {
                map[resultSet.getInt("commandId")] = ChannelCommandState.valueOf(resultSet.getString("state"))
            }
        }, channelId)
        disabled.accept(map)
    }

    fun contains(channelId: Long, commandId: Int, contains: Consumer<Boolean>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ? AND commandId = ?", Consumer {
            contains.accept(it.next())
        }, channelId, commandId)
    }

    fun insert(guildId: Long, channelId: Long, commandId: Int, state: ChannelCommandState) {
        driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, channelId, commandId) VALUES (?, ?, ?)",
                guildId, channelId, commandId, state.toString())
    }
}