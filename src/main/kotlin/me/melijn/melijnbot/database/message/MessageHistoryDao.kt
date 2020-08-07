package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MessageHistoryDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "historyMessages"
    override val tableStructure: String = "guildId bigint, textChannelId bigint, authorId bigint, messageId bigint, content varchar(2048), moment bigint"
    override val primaryKey: String = "messageId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(daoMessage: DaoMessage) {
        daoMessage.run {
            driverManager.executeUpdate("INSERT INTO $table (guildId, textChannelId, authorId, messageId, content, moment) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET content = ?",
                guildId, textChannelId, authorId, messageId, content, moment, content)
        }
    }

    fun add(daoMessage: DaoMessage) {
        daoMessage.run {
            driverManager.executeUpdate("INSERT INTO $table (guildId, textChannelId, authorId, messageId, content, moment) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
                guildId, textChannelId, authorId, messageId, content, moment)
        }
    }

    suspend fun get(messageId: Long): DaoMessage? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE messageId = ?", { rs ->
            val result = if (rs.next()) {
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
            it.resume(result)
        }, messageId)
    }

    fun clearOldMessages() {
        driverManager.executeUpdate("DELETE FROM $table WHERE moment < ?", (System.currentTimeMillis() - 86_400_000 * 7))
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