package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PlaylistDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "playlists"
    override val tableStructure: String = "id bigint, playlist varchar(32)"
    override val primaryKey: String = "id, playlist"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun insert(id: Long, playlist: String) {
        driverManager.executeUpdate("INSERT INTO $table (id, playlist) VALUES (?, ?)",
            id, playlist)
    }

    suspend fun delete(id: Long, playlist: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE id=? AND playlist=?",
            id, playlist)
    }

    //Sign pls

    suspend fun getPlaylists(id: Long): List<String> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table", { rs ->
            val playlists = mutableListOf<String>()
            while (rs.next()) {
                playlists.add(rs.getString("playlist"))
            }
            it.resume(playlists)
        })
    }

}