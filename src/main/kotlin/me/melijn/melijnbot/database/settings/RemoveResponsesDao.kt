package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RemoveResponsesDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "removeResponses"
    override val tableStructure: String = "guildId bigint, channelId bigint, seconds int"
    override val primaryKey: String = "channelId"

    override val cacheName: String = "remove:response"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun remove(guildId: Long, channelId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND channelId = ?", guildId, channelId)
    }

    fun insert(guildId: Long, channelId: Long, seconds: Int) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelId, seconds) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET seconds = ?",
            guildId, channelId, seconds, seconds)
    }

    suspend fun getChannels(guildId: Long): Map<Long, Int> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val ls = mutableMapOf<Long, Int>()
            while (rs.next()) {
                ls[rs.getLong("channelId")] = rs.getInt("seconds")
            }
            it.resume(ls)
        }, guildId)
    }
}