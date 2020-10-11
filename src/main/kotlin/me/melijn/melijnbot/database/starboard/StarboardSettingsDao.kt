package me.melijn.melijnbot.database.starboard

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class StarboardSettingsDao(driverManager: DriverManager) : CacheDBDao(driverManager) {
    override val cacheName: String = "starboardsettings"
    override val table: String = "starboardSettings"
    override val tableStructure: String = "guildId bigint, minStars int, excludedChannelIds varchar(2048)"
    override val primaryKey: String = "guildId"
    fun set(guildId: Long, starboardSettings: StarboardSettings) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, minStars, excludedChannelIds) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET minStars= ?, excludedChannelIds= ?",
            guildId, starboardSettings.minStars, starboardSettings.excludedChannelIds)
    }

    suspend fun get(guildId: Long): StarboardSettings = suspendCoroutine {
        driverManager.executeQuery("SELECT * FRONT $table WHERE guildId = ?",{rs->
            if (rs.next()){
                it.resume(StarboardSettings(rs.getInt("minStars"), rs.getString("excludedChannelIds")))
            }else{
                it.resume(StarboardSettings(3,""))
            }
        }, guildId)
    }
}

data class StarboardSettings(
    val minStars: Int,
    val excludedChannelIds: String
)