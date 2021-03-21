package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MessageDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "messages"
    override val tableStructure: String = "guild_id bigint, msg_name varchar(64), content varchar(6096)"
    override val primaryKey: String = "guild_id, msg_name"

    override val cacheName: String = "messages"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(guildId: Long, msgName: String, content: String) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guild_id, msg_name, content) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET content = ?",
            guildId, msgName, content, content
        )
    }

    fun remove(guildId: Long, msgName: String) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guild_id = ? AND msg_name = ?",
            guildId, msgName
        )
    }

    suspend fun get(guildId: Long, msgName: String): String? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guild_id = ? AND msg_name = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("content"))
            } else it.resume(null)
        }, guildId, msgName)
    }

    suspend fun getMessages(guildId: Long): List<String> = suspendCoroutine {
        driverManager.executeQuery("SELECT msg_name FROM $table WHERE guild_id = ?", { rs ->
            val list = mutableListOf<String>()
            while(rs.next()) {
                list.add(rs.getString("msg_name"))
            }
            it.resume(list)
        }, guildId)
    }
}