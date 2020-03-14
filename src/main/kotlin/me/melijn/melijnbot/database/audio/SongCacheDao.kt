package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SongCacheDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "songCache"
    override val tableStructure: String = "song varchar(2048), track varchar(1024), hits int, time bigint"
    override val primaryKey: String = "song"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun getTrackInfo(song: String): String? = suspendCoroutine {
        driverManager.executeQuery("SELECT track FROM $table WHERE song = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("track"))
            } else {
                it.resume(null)
            }
        }, song)
    }

    suspend fun addTrack(song: String, track: String) {
        driverManager.executeUpdate(
            "INSERT INTO $table (song, track, hits, time) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET track = ?, hits = $table.hits + 1",
            song, track, 1, System.currentTimeMillis(), track)
    }

    suspend fun clearOldTracks() {
        driverManager.executeUpdate("DELETE FROM $table WHERE hits = 1 AND time < ?", System.currentTimeMillis() - (86_400_000 * 3))
        driverManager.executeUpdate("DELETE FROM $table WHERE hits > 1 AND time < ?", System.currentTimeMillis() - (86_400_000 * 7))
    }
}