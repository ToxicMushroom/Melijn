package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TracksDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "audioTracks"
    override val tableStructure: String = "guildId bigint, position int, track varchar(2048), trackData varchar(2048)"
    override val primaryKey: String = "guildId, position"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(guildId: Long, position: Int, track: String, trackData: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, position, track, trackData) VALUES (?, ?, ?, ?)",
            guildId, position, track, trackData)
    }

    suspend fun getMap(): Map<Long, Map<Int, Pair<String, String>>> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table", { rs ->
            val guildTracksMap = mutableMapOf<Long, Map<Int, Pair<String, String>>>()
            while (rs.next()) {
                val guildId = rs.getLong("guildId")
                val trackMap = guildTracksMap.getOrDefault(guildId, emptyMap()).toMutableMap()
                trackMap[rs.getInt("position")] = Pair(rs.getString("track"), rs.getString("trackData"))
                guildTracksMap[guildId] = trackMap
            }
            it.resume(guildTracksMap)
        })
    }
}