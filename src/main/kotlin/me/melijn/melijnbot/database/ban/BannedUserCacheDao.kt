package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.database.CacheDao
import me.melijn.melijnbot.database.DriverManager
import java.util.concurrent.TimeUnit

class BannedUserCacheDao(driverManager: DriverManager): CacheDao(driverManager) {

    override val cacheName: String = "banned_users"

    fun add(user: Long, guild: Long) {
        driverManager.setCacheEntry("${guild}:${user}", "TRUE", 5, TimeUnit.SECONDS)
    }

    suspend fun contains(user: Long, guild: Long): Boolean {
        return driverManager.getCacheEntry("${guild}:${user}") == "TRUE"
    }

}
