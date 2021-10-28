package me.melijn.melijnbot.database.validemojis

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ValidEmojiDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "valid_emojis"
    override val tableStructure: String = "emoji text, data text"
    override val primaryKey: String = "emoji"
    override val cacheName: String = "valid_emojis"


    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(emoji: String) {
        driverManager.executeUpdate("INSERT INTO $table (emoji, data) VALUES (?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            emoji, "")
    }

    suspend fun isValid(emoji: String): Boolean = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE emoji = ?",
            { rs ->
                it.resume(rs.next())
            }, emoji
        )
    }
}