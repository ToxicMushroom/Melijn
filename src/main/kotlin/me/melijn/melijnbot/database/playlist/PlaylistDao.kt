package me.melijn.melijnbot.database.playlist

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PlaylistDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "playlists"
    override val tableStructure: String = "userId bigint, playlist varchar(128), id int, track varchar(2048)"
    override val primaryKey: String = "userId, playlist, id, track"
    override val cacheName: String = "userId, playlist, id"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }


    fun set(userId: Long, playlist: String, id: Int, track: String) {
        driverManager.executeUpdate("INSERT INTO $table (userId, playlist, id, track) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET track ")
    }

    fun removeById(userId: Long, playlist: String, id: Int) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ? AND playlist = ? AND id = ?")
    }

    fun removeByTrack(userId: Long, playlist: String, track: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ? AND playlist = ? AND id = ?")
    }


    suspend fun getPlaylists(userId: Long): Map<String, Map<Int, String>> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { rs ->
            val map = mutableMapOf<String, Map<Int, String>>()
            while (rs.next()) {
                val playlist  = rs.getString("playlist")
                val subMap = map[playlist]?.toMutableMap() ?: mutableMapOf()
                subMap[rs.getInt("id")] = rs.getString("track")
                map[playlist] = subMap
            }
            it.resume(map)
        }, userId)
    }

    suspend fun getPlaylist(userId: Long, playlist: String): Map<Int, String> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ? AND playlist = ?", { rs ->
            val map = mutableMapOf<Int, String>()
            while (rs.next()) {
                map[rs.getInt("id")] = rs.getString("track")
            }
            it.resume(map)
        }, userId, playlist)
    }
}