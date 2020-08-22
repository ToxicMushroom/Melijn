package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LastVoiceChannelDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "lastVoiceChannels"
    override val tableStructure: String = "guildId bigint, channelId bigint"
    override val primaryKey: String = "guildId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun add(guildId: Long, channelId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, channelId) VALUES (?, ?)",
            guildId, channelId)
    }

    suspend fun getMap(): Map<Long, Long> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table", { rs ->
            val map = mutableMapOf<Long, Long>()
            while (rs.next()) {
                map[rs.getLong("guildId")] = rs.getLong("channelId")
            }
            it.resume(map)
        })
    }
}