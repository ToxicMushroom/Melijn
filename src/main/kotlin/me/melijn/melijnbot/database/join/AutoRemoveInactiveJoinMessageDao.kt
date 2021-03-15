package me.melijn.melijnbot.database.join

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AutoRemoveInactiveJoinMessageDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "autoremove_inactive_joinmessage_durations"
    override val tableStructure: String = "guild_id bigint, duration bigint"
    override val primaryKey: String = "guild_id"

    override val cacheName: String = "autoremoveijms"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(guildId: Long, duration: Long) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guild_id, duration) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET duration = ?",
            guildId, duration, duration
        )
    }

    suspend fun get(guildId: Long): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guild_id = ?", { rs ->
            if (rs.next()) it.resume(rs.getLong("duration"))
            else it.resume(-1)
        }, guildId)
    }

    fun delete(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guild_id = ?", guildId)
    }
}