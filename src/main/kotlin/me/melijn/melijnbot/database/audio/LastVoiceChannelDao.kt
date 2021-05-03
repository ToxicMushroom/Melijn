package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.models.PodInfo
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
        driverManager.executeUpdate(
            "INSERT INTO $table (guildId, channelId) VALUES (?, ?)",
            guildId, channelId
        )
    }

    suspend fun getMap(podInfo: PodInfo): Map<Long, Long> = suspendCoroutine {
        val clause = podInfo.shardList.joinToString(", ") { "?" }

        val query = "SELECT * FROM $table WHERE ((guildId >> 22) % ${podInfo.shardCount}) IN ($clause)"
        driverManager.executeQuery(query, { rs ->
            val map = mutableMapOf<Long, Long>()
            while (rs.next()) {
                map[rs.getLong("guildId")] = rs.getLong("channelId")
            }
            it.resume(map)
        }, *podInfo.shardList.toTypedArray())
    }

    fun clear(podInfo: PodInfo) {
        val clause = podInfo.shardList.joinToString(", ") { "?" }

        driverManager.executeUpdate(
            "DELETE FROM $table WHERE ((guildId >> 22) % ${podInfo.shardCount}) IN ($clause)",
            *podInfo.shardList.toTypedArray()
        )
    }
}