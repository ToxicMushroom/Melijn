package me.melijn.melijnbot.database.starboard

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class StarboardMessageDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val cacheName: String = "starboardmsg"
    override val table: String = "starboardMessages"
    override val tableStructure: String = "guildId bigint, channelId bigint, authorId bigint, messageId bigint, starboardMessageId bigint, stars int, deleted boolean, moment bigint"
    override val primaryKey: String = "messageId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(guildId: Long, channelId: Long, authorId: Long, messageId: Long, starboardMessageId: Long, stars: Int, deleted: Boolean, moment: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelId, authorId, messageId, starboardMessageId, stars, deleted, moment) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET stars = ?",
            guildId, channelId, authorId, messageId, starboardMessageId, stars, deleted, moment, stars)
    }

    suspend fun getStarboardInfo(messageId: Long): StarboardInfo? = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE messageId = ? OR starboardMessageId = ?",
            { rs ->
                if (rs.next()) {
                    it.resume(StarboardInfo(
                        rs.getLong("starboardMessageId"),
                        rs.getInt("stars"),
                        rs.getBoolean("deleted"),
                        rs.getLong("moment")
                    ))
                } else {
                    it.resume(null)
                }
            }, messageId, messageId
        )
    }
}

data class StarboardInfo(
    val starboardMessageId: Long,
    val stars: Int,
    val deleted: Boolean,
    val moment: Long
)