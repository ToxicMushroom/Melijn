package me.melijn.melijnbot.database.channel

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.ChannelType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChannelDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "channels"
    override val tableStructure: String = "guildId bigint, channelType varchar(32), channelId bigint"
    override val primaryKey: String = "guildId, channelType"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun set(guildId: Long, channelType: ChannelType, channelId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelType, channelId) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET channelId = ?",
            guildId, channelType.toString(), channelId, channelId)
    }

    suspend fun remove(guildId: Long, channelType: ChannelType) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND channelType = ?", guildId, channelType.toString())
    }

    suspend fun get(guildId: Long, channelType: ChannelType): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND channelType = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getLong("channelId"))
            } else {
                it.resume(-1L)
            }
        }, guildId, channelType.toString())
    }

    suspend fun getChannels(channelType: ChannelType): Map<Long, Long> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE channelType = ?", { rs ->
            val roleMap = HashMap<Long, Long>()
            while (rs.next()) {
                roleMap[rs.getLong("guildId")] = rs.getLong("channelId")
            }
            it.resume(roleMap)

        }, channelType.toString())
    }
}