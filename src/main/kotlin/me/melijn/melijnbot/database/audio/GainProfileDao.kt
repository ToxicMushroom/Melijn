package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GainProfileDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "gain_profiles"
    override val tableStructure: String = "id bigint, name varchar(32), profile double precision[]"
    override val primaryKey: String = "id, name"

    override val cacheName: String = "gainprofile"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun insert(id: Long, name: String, profile: FloatArray) {
        driverManager.executeUpdate(
            "INSERT INTO $table (id, name, profile) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET profile = ?",
            id, name, profile, profile
        )
    }

    suspend fun get(id: Long): Map<String, GainProfile> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE id = ?", { rs ->
            val map = mutableMapOf<String, GainProfile>()

            while (rs.next()) {
                map[rs.getString("name")] = GainProfile.fromString(
                    rs.getString("profile").removeSurrounding("{", "}")
                )
            }

            it.resume(map)
        }, id)
    }

    fun delete(guildId: Long, profileName: String) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE id = ? AND name = ?",
            guildId, profileName
        )
    }

}

data class GainProfile(
    val band0: Float = 0.0f, // 25Hz
    val band1: Float = 0.0f, // 40Hz
    val band2: Float = 0.0f, // 63Hz
    val band3: Float = 0.0f, // 100Hz
    val band4: Float = 0.0f, // 160Hz
    val band5: Float = 0.0f, // 250Hz
    val band6: Float = 0.0f, // 400Hz
    val band7: Float = 0.0f, // 630Hz
    val band8: Float = 0.0f, // 1kHz
    val band9: Float = 0.0f, // 1.6kHz
    val band10: Float = 0.0f, // 2.5kHz
    val band11: Float = 0.0f, // 4kHz
    val band12: Float = 0.0f, // 6.3kHz
    val band13: Float = 0.0f, // 10kHz
    val band14: Float = 0.0f // 16kHz
) {

    fun toFloatArray(): FloatArray = floatArrayOf(
        band0, band1, band2, band3, band4, band5, band6, band7, band8, band9, band10, band11, band12, band13, band14
    )

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