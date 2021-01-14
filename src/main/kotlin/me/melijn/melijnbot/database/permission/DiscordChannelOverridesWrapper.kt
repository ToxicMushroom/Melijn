package me.melijn.melijnbot.database.permission

class DiscordChannelOverridesWrapper(private val discordChannelOverridesDao: DiscordChannelOverridesDao) {


    fun put(guildId: Long, channelId: Long, id: Long, denied: Long, allowed: Long) {
        discordChannelOverridesDao.put(guildId, channelId, id, denied, allowed)
    }

    fun bulkPut(guildId: Long, channelId: Long, map: Map<Long, Pair<Long, Long>>) {
        discordChannelOverridesDao.bulkPut(guildId, channelId, map)
    }

    fun remove(guildId: Long, channelId: Long) {
        discordChannelOverridesDao.remove(guildId, channelId)
    }

    suspend fun getAll(guildId: Long, channelId: Long): Map<Long, Pair<Long, Long>> {
        return discordChannelOverridesDao.getAll(guildId, channelId)
    }

    fun removeAll(guildId: Long, list: List<Long>) {
         discordChannelOverridesDao.removeAll(guildId, list)
    }
}