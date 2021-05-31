package me.melijn.melijnbot.database.join

import me.melijn.melijnbot.database.CacheDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.database.getValueFromCache
import java.util.concurrent.TimeUnit

class InactiveJMDao(driverManager: DriverManager) : CacheDao(driverManager) {

    override val cacheName: String = "inactiveJMS"

    fun new(guildId: Long, userId: Long, channelMsgPair: Pair<Long, Long>, duration: Long) {
        setCacheEntry("$guildId:$userId", channelMsgPair, (duration).toInt(), TimeUnit.SECONDS)
    }

    suspend fun getMsg(guildId: Long, userId: Long): Pair<Long, Long>? {
        return getValueFromCache("$guildId:$userId")
    }

    fun delete(guildId: Long, userId: Long) {
        removeCacheEntry("$guildId:$userId")
    }
}