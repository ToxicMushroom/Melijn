package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class MessageDao(val driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "messages"
    override val tableStructure: String = "guildId bigint, textChannelId bigint, authorId bigint, messageId bigint, content varchar(2048), moment bigint"
    override val keys: String = "PRIMARY KEY (messageId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun add(daoMessage: DaoMessage) {
        daoMessage.run {
            driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId, textChannelId, authorId, messageId, content, moment) VALUES (?, ?, ?, ?, ?, ?)",
                    guildId, textChannelId, authorId, messageId, content, moment)
        }
    }

    fun get(messageId: Long, message: (DaoMessage?) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE messageId = ?", { rs ->
            if (rs.next()) {
                message(DaoMessage(
                        rs.getLong("guildId"),
                        rs.getLong("textChannelId"),
                        rs.getLong("authorId"),
                        messageId,
                        rs.getString("content"),
                        rs.getLong("moment")
                ))
            } else {
                message(null)
            }
        }, messageId)
    }
}

data class DaoMessage(
        val guildId: Long,
        val textChannelId: Long,
        val authorId: Long,
        val messageId: Long,
        val content: String,
        val moment: Long
)