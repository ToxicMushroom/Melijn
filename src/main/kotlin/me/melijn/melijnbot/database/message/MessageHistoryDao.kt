package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class MessageHistoryDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "historyMessages"
    override val tableStructure: String = "guildId bigint, textChannelId bigint, authorId bigint, messageId bigint, content varchar(2048), moment bigint"
    override val keys: String = "PRIMARY KEY (messageId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun set(daoMessage: DaoMessage) {
        daoMessage.run {
            driverManager.executeUpdate("INSERT INTO $table (guildId, textChannelId, authorId, messageId, content, moment) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE content = ?",
                guildId, textChannelId, authorId, messageId, content, moment, content)
        }
    }

    suspend fun add(daoMessage: DaoMessage) {
        daoMessage.run {
            driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, textChannelId, authorId, messageId, content, moment) VALUES (?, ?, ?, ?, ?, ?)",
                guildId, textChannelId, authorId, messageId, content, moment)
        }
    }

    suspend fun get(messageId: Long): DaoMessage? {
        driverManager.executeQuery("SELECT * FROM $table WHERE messageId = ?", messageId).use { rs ->
            return if (rs.next()) {
                DaoMessage(
                    rs.getLong("guildId"),
                    rs.getLong("textChannelId"),
                    rs.getLong("authorId"),
                    messageId,
                    rs.getString("content"),
                    rs.getLong("moment")
                )
            } else {
                null
            }
        }
    }

}

data class DaoMessage(
    val guildId: Long,
    val textChannelId: Long,
    val authorId: Long,
    val messageId: Long,
    var content: String,
    val moment: Long
)