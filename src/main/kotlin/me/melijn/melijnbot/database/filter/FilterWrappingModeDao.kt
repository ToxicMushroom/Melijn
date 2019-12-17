package me.melijn.melijnbot.database.filter

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.FilterMode
import me.melijn.melijnbot.objects.utils.enumValueOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FilterWrappingModeDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "filterWrappingModes"
    override val tableStructure: String = "guildId bigint, channelId bigint, mode varchar(64)"
    override val primaryKey: String = "guildId, channelId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun set(guildId: Long, channelId: Long?, mode: FilterMode) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelId, mode) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET mode = ?",
            guildId, channelId ?: -1, mode.toString(), mode.toString())
    }

    suspend fun get(guildId: Long, channelId: Long?): FilterMode = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND channelId = ?", { rs ->
            if (rs.next()) {
                it.resume(enumValueOrNull(rs.getString("mode")) ?: FilterMode.NO_MODE)
            } else {
                it.resume(FilterMode.NO_MODE)
            }
        }, guildId, channelId ?: -1)
    }

    suspend fun unset(guildId: Long, channelId: Long?) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND channelId = ?",
            guildId, channelId ?: -1)
    }
}