package me.melijn.melijnbot.database.disabled

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.util.function.Consumer

class DisabledChannelCommandDao(val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "disabledChannelCommands"
    override val tableStructure: String = "guildId bigint, channelId bigint, commandId int"
    override val keys: String = "UNIQUE KEY (channelId, commandId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(channelId: Long, disabled: Consumer<Set<Int>>) {
        val list = HashSet<Int>()
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ?", Consumer { resultSet ->
            while (resultSet.next()) {
                list.add(resultSet.getInt("commandId"))
            }
        }, channelId)
        disabled.accept(list.toSet())
    }

    fun contains(channelId: Long, commandId: Int, contains: Consumer<Boolean>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ? AND commandId = ?", Consumer {
            contains.accept(it.next())
        }, channelId, commandId)
    }

    fun insert(guildId: Long, channelId: Long, commandId: Int) {
        driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, channelId, commandId) VALUES (?, ?, ?)",
                guildId, channelId, commandId)
    }
}