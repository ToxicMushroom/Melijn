package me.melijn.melijnbot.database.ratelimit

import me.melijn.melijnbot.database.CacheDao
import me.melijn.melijnbot.database.DriverManager

class RatelimitWrapper(driverManager: DriverManager) : CacheDao(driverManager) {
    override val cacheName: String = "GLOBAL_RATELIMIT"

    fun set(count: Long){
        driverManager.setCacheEntry(cacheName, count.toString(), null)
    }


    suspend fun get(): Long? {
        return driverManager.getCacheEntry(cacheName)?.toLong()
    }
}