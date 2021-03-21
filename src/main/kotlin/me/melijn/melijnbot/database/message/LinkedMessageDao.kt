package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.MessageType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LinkedMessageDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "linked_messages"
    override val tableStructure: String = "guild_id bigint, type varchar(32), linked_message varchar(64)"
    override val primaryKey: String = "guild_id, type"

    override val cacheName: String = "linked_messages"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(guildId: Long, type: MessageType, linkedMessage: String) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guild_id, type, linked_message) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET linked_message = ?",
            guildId, type.toString(), linkedMessage, linkedMessage
        )
    }

    fun remove(guildId: Long, type: MessageType) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guild_id = ? AND type = ?",
            guildId, type.toString()
        )
    }

    suspend fun get(guildId: Long, type: MessageType): String? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guild_id = ? AND type = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("linked_message"))
            } else it.resume(null)
        }, guildId, type.toString())
    }
}