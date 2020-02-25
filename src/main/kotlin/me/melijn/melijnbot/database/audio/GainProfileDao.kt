package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GainProfileDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "gainProfiles"
    override val tableStructure: String = "id bigint, name varchar(32), profile varchar(128)"
    override val primaryKey: String = "id, name"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun insert(id: Long, name: String, profile: String) {
        driverManager.executeUpdate("INSERT INTO $table (id, name, profile) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET profile = ?",
            id, name, profile, profile)
    }

    suspend fun get(id: Long): Map<String, GainProfile> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE id = ?", { rs ->
            val map = mutableMapOf<String, GainProfile>()

            while (rs.next()) {
                map[rs.getString("name")] = GainProfile.fromString(rs.getString("profile"))
            }

            it.resume(map)
        }, id)
    }

    suspend fun delete(guildId: Long, profileName: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE id = ? AND name = ?",
            guildId, profileName)
    }

}

data class GainProfile(
    val band0: Float = 0.0f,
    val band1: Float = 0.0f,
    val band2: Float = 0.0f,
    val band3: Float = 0.0f,
    val band4: Float = 0.0f,
    val band5: Float = 0.0f,
    val band6: Float = 0.0f,
    val band7: Float = 0.0f,
    val band8: Float = 0.0f,
    val band9: Float = 0.0f,
    val band10: Float = 0.0f,
    val band11: Float = 0.0f,
    val band12: Float = 0.0f,
    val band13: Float = 0.0f,
    val band14: Float = 0.0f
) {

    companion object {
        fun fromString(string: String): GainProfile {
            val floats = string.split(",").map { it.toFloat() }
            return GainProfile(
                floats[0],
                floats[1],
                floats[2],
                floats[3],
                floats[4],
                floats[5],
                floats[6],
                floats[7],
                floats[8],
                floats[9],
                floats[10],
                floats[11],
                floats[12],
                floats[13],
                floats[14]
            )
        }
    }
}