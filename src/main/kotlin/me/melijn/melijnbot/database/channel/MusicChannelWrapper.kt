package me.melijn.melijnbot.database.channel

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class MusicChannelWrapper(private val musicChannelDao: MusicChannelDao) {


    suspend fun getChannel(guildId: Long): Long {
        val cached = musicChannelDao.getCacheEntry(guildId, HIGHER_CACHE)?.toLong()
        if (cached != null) return cached

        val result = musicChannelDao.get(guildId)
        musicChannelDao.setCacheEntry(guildId, result, NORMAL_CACHE)
        return result
    }

    fun removeChannel(guildId: Long) {
        musicChannelDao.remove(guildId)
        musicChannelDao.setCacheEntry(guildId, -1, NORMAL_CACHE)
    }

    fun setChannel(guildId: Long, channelId: Long) {
        musicChannelDao.set(guildId, channelId)
        musicChannelDao.setCacheEntry(guildId, channelId, NORMAL_CACHE)
    }
}