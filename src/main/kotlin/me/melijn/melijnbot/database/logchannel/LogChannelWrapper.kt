package me.melijn.melijnbot.database.logchannel

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType

class LogChannelWrapper(private val logChannelDao: LogChannelDao) {

    suspend fun getChannelId(guildId: Long, logChannelType: LogChannelType): Long {
        val result = logChannelDao.getCacheEntry("$logChannelType:$guildId", HIGHER_CACHE)?.toLong()
        if (result != null) return result

        val channel = logChannelDao.get(guildId, logChannelType)
        logChannelDao.setCacheEntry("$logChannelType:$guildId", channel, NORMAL_CACHE)
        return channel
    }

    fun removeChannel(guildId: Long, logChannelType: LogChannelType) {
        logChannelDao.unset(guildId, logChannelType)
        logChannelDao.setCacheEntry("$logChannelType:$guildId", -1, NORMAL_CACHE)
    }

    fun removeChannels(guildId: Long, logChannelTypes: List<LogChannelType>) {
        logChannelDao.bulkRemove(guildId, logChannelTypes)
        for (type in logChannelTypes) {
            logChannelDao.setCacheEntry("$type:$guildId", -1, NORMAL_CACHE)
        }
    }

    fun setChannels(guildId: Long, logChannelTypes: List<LogChannelType>, channelId: Long) {
        logChannelDao.bulkPut(guildId, logChannelTypes, channelId)
        for (type in logChannelTypes) {
            logChannelDao.setCacheEntry("$type:$guildId", channelId, NORMAL_CACHE)
        }
    }

    fun setChannel(guildId: Long, logChannelType: LogChannelType, channelId: Long) {
        logChannelDao.set(guildId, logChannelType, channelId)
        logChannelDao.setCacheEntry("$logChannelType:$guildId", channelId, NORMAL_CACHE)
    }

    fun migrateChannel(oldId: Long, newId: Long) {
        logChannelDao.migrateChannel(oldId, newId)
    }

    fun invalidate(guildId: Long) {
        LogChannelType.values().forEach { channel ->
            logChannelDao.removeCacheEntry("$channel:$guildId")
        }
    }
}