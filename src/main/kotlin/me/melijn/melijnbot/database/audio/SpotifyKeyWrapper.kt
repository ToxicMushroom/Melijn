package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.CacheDao
import me.melijn.melijnbot.database.DriverManager
import java.util.concurrent.TimeUnit

class SpotifyKeyWrapper(driverManager: DriverManager) : CacheDao(driverManager) {

    override val cacheName: String = "SPOTIFY_KEY"

    fun update(newKey: String) {
        setCacheEntry("token", newKey, 40, TimeUnit.MINUTES)
    }

    suspend fun get(): String? {
        return getCacheEntry("token")
    }
}