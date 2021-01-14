package me.melijn.melijnbot.database.channel

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.ChannelType

class ChannelWrapper(private val channelDao: ChannelDao) {

    suspend fun getChannelId(guildId: Long, channelType: ChannelType): Long {
        val cached = channelDao.getCacheEntry("$channelType:$guildId", HIGHER_CACHE)?.toLong()
        if (cached != null) return cached

        val result = channelDao.get(guildId, channelType)
        channelDao.setCacheEntry("$channelType:$guildId", result, NORMAL_CACHE)
        return result
    }

    fun removeChannel(guildId: Long, channelType: ChannelType) {
        channelDao.remove(guildId, channelType)
        channelDao.setCacheEntry("$channelType:$guildId", -1, NORMAL_CACHE)
    }

    fun setChannel(guildId: Long, channelType: ChannelType, channelId: Long) {
        channelDao.set(guildId, channelType, channelId)
        channelDao.setCacheEntry("$channelType:$guildId", channelId, NORMAL_CACHE)
    }

    suspend fun getChannels(channelType: ChannelType): Map<Long, Long> {
        return channelDao.getChannels(channelType)
    }

    fun migrateChannel(oldId: Long, newId: Long) {
        channelDao.migrateChannel(oldId, newId)
    }

    fun invalidate(guildId: Long) {
        ChannelType.values().forEach { channel ->
            channelDao.removeCacheEntry("$channel:$guildId")
        }
    }
}