package me.melijn.melijnbot.database.cooldown

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.util.function.Consumer

class CommandChannelCooldownDao(val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "commandChannelCooldowns"
    override val tableStructure: String = "guildId bigint, channelId bigint, commandId int, cooldownMillis long"
    override val keys: String = "UNIQUE KEY (channelId, commandId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun getCooldownMapForChannel(channelId: Long, cooldownMap: Consumer<Map<Int, Long>>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE channelId = ?", Consumer {
            val map = HashMap<Int, Long>()
            while (it.next()) {
                map[it.getInt("commandId")] = it.getLong("cooldownMillis")
            }
            cooldownMap.accept(map)
        }, channelId)
    }
}