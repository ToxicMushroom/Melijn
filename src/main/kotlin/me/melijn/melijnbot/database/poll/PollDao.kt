package me.melijn.melijnbot.database.poll

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class PollDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "polls"
    override val tableStructure: String = "guildId bigint, channelId bigint, messageId bigint, moment bigint, endTime bigint"
    override val primaryKey: String = "messageId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun add(guildId: Long, channelId: Long, messageId: Long, moment: Long, endTime: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelId, messageId, moment, endTime) VALUES (?, ?, ?, ?, ?)",
            guildId, channelId, messageId, moment, endTime)
    }

    fun remove(guildId: Long, channelId: Long, messageId: Long, moment: Long, endTime: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelId, messageId, moment, endTime) VALUES (?, ?, ?, ?, ?)",
            guildId, channelId, messageId, moment, endTime)
    }
}