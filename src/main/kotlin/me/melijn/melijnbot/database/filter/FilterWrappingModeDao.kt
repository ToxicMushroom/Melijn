package me.melijn.melijnbot.database.filter

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.WrappingMode
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

    suspend fun set(guildId: Long, channelId: Long?, mode: WrappingMode) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelId, mode) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET mode = ?",
            guildId, channelId, mode, mode)
    }

    suspend fun get(guildId: Long, channelId: Long?): WrappingMode = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND channelId = ?", {rs ->
            if (rs.next()) {
                it.resume(enumValueOrNull(rs.getString("mode")) ?: WrappingMode.DEFAULT)
            } else {
                it.resume(WrappingMode.DEFAULT)
            }
        }, guildId, channelId)
    }

    suspend fun unset(guildId: Long, channelId: Long?) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND channelId = ?", guildId, channelId)
    }
}