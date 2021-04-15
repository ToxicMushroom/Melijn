package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.utils.splitIETEL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MessageHistoryDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "history_messages"
    override val tableStructure: String =
        "guild_id bigint, channel_id bigint, author_id bigint, message_id bigint, content varchar(2048), embed varchar(6200), attachments varchar(1024), moment bigint"
    override val primaryKey: String = "message_id"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(daoMessage: DaoMessage) {
        val sql =
            "INSERT INTO $table (guild_id, channel_id,>pay author_id, message_id, content, embed, attachments, moment) VALUES (?, ?, ?, ?, ?, ?, ?, ?)" +
                    " ON CONFLICT ($primaryKey) DO UPDATE SET content = ?, embed = ?"
        daoMessage.run {
            driverManager.executeUpdate(
                sql,
                guildId,
                textChannelId,
                authorId,
                messageId,
                content,
                embed,
                attachments.joinToString("///"),
                moment,
                content,
                embed
            )
        }
    }

    fun add(daoMessage: DaoMessage) {
        val sql =
            "INSERT INTO $table (guild_id, channel_id, author_id, message_id, content, embed, attachments, moment) VALUES (?, ?, ?, ?, ?, ?, ?, ?)" +
                    " ON CONFLICT ($primaryKey) DO NOTHING"
        daoMessage.run {
            driverManager.executeUpdate(
                sql,
                guildId, textChannelId, authorId, messageId, content, embed, attachments.joinToString("///"), moment
            )
        }
    }

    suspend fun get(messageId: Long): DaoMessage? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE message_id = ?", { rs ->
            val result = if (rs.next()) {
                DaoMessage(
                    rs.getLong("guild_id"),
                    rs.getLong("channel_id"),
                    rs.getLong("author_id"),
                    messageId,
                    rs.getString("content"),
                    rs.getString("embed"),
                    rs.getString("attachments").splitIETEL("///"),
                    rs.getLong("moment")
                )
            } else {
                null
            }
            it.resume(result)
        }, messageId)
    }

    fun clearOldMessages() {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE moment < ?",
            (System.currentTimeMillis() - 86_400_000 * 7)
        )
    }

    suspend fun getMany(map: List<Long>): List<DaoMessage> = suspendCoroutine {
        if (map.isEmpty()) {
            it.resume(emptyList())
            return@suspendCoroutine
        }
        val clause = map.joinToString(", ") { "?" }
        val list = mutableListOf<DaoMessage>()
        driverManager.executeQueryList("SELECT * FROM $table WHERE messageId IN ($clause)", { rs ->
            while (rs.next()) {
                list.add(
                    DaoMessage(
                        rs.getLong("guild_id"),
                        rs.getLong("channel_id"),
                        rs.getLong("author_id"),
                        rs.getLong("message_id"),
                        rs.getString("content"),
                        rs.getString("embed"),
                        rs.getString("attachments").splitIETEL("///"),
                        rs.getLong("moment")
                    )
                )
            }
            it.resume(list)
        }, map)
    }

    fun add(daoMessages: List<DaoMessage>) {
        val sql =
            "INSERT INTO $table (guild_id, channel_id, author_id, message_id, content, embed, attachments, moment) VALUES (?, ?, ?, ?, ?, ?, ?, ?)" +
                    " ON CONFLICT ($primaryKey) DO NOTHING"
        driverManager.getUsableConnection { connection ->
            connection.prepareStatement(sql).use { prep ->
                for (daoMessage in daoMessages) {
                    daoMessage.run {
                        prep.setLong(1, guildId)
                        prep.setLong(2, textChannelId)
                        prep.setLong(3, authorId)
                        prep.setLong(4, messageId)
                        prep.setString(5, content)
                        prep.setString(6, embed)
                        prep.setString(7, attachments.joinToString("///"))
                        prep.setLong(8, moment)
                        prep.addBatch()
                    }
                }
                prep.executeBatch()
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
    var embed: String,
    var attachments: List<String>,
    val moment: Long
)