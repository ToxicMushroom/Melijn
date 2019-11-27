package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TracksDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "audioTracks"
    override val tableStructure: String = "guildId bigint, position int, track varchar(2048)"
    override val primaryKey: String = "guildId, position"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun set(guildId: Long, position: Int, track: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, position, track) VALUES (?, ?, ?)",
            guildId, position, track)
    }


    suspend fun get(guildId: Long, position: Int): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND position = ?", { rs ->
            it.resume(if (rs.next()) {
                rs.getString("track")
            } else {
                ""
            })
        }, guildId, position)
    }

    suspend fun getMap(): Map<Long, List<Pair<Int, String>>> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table", { rs ->
            val map = mutableMapOf<Long, List<Pair<Int, String>>>()
            while (rs.next()) {
                val guildId = rs.getLong("guildId")
                val list = map.getOrDefault(guildId, emptyList()).toMutableList()
                list.add(Pair(rs.getInt("position"), rs.getString("track")))
                map[guildId] = list
            }
            it.resume(map)
        })
    }
}