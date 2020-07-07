package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RemoveInvokeDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "removeInvoke"
    override val tableStructure: String = "guildId bigint, channelId bigint, seconds int"
    override val primaryKey: String = "channelId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun remove(guildId: Long, channelId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND channelId = ?", guildId, channelId)
    }

    suspend fun insert(guildId: Long, channelId: Long, seconds: Int) {
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