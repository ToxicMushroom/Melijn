package me.melijn.melijnbot.database.scripts

import me.melijn.melijnbot.database.CacheDao
import me.melijn.melijnbot.database.DriverManager
import java.util.concurrent.TimeUnit

class ScriptCooldownDao(driverManager: DriverManager) : CacheDao(driverManager) {

    override val cacheName: String = "scriptscooldown"

    fun addCooldown(guildId: Long, invoke: String, seconds: Int) {
        setCacheEntry("$guildId:$invoke", true, seconds, TimeUnit.SECONDS)
    }

    suspend fun isOnCooldown(guildId: Long, invoke: String): Boolean {
        return getCacheEntry("$guildId:$invoke") != null
    }
}
