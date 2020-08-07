package me.melijn.melijnbot.database.prefix

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.internals.utils.splitIETEL

class GuildPrefixWrapper(private val guildPrefixDao: GuildPrefixDao) {

    suspend fun getPrefixes(guildId: Long): List<String> {
        val result = guildPrefixDao.getCacheEntry(guildId, HIGHER_CACHE)?.splitIETEL("%SPLIT%")

        if (result != null) return result

        val prefixes = guildPrefixDao.get(guildId)
        guildPrefixDao.setCacheEntry(guildId, prefixes, NORMAL_CACHE)
        return prefixes.splitIETEL("%SPLIT%")
    }

    suspend fun addPrefix(guildId: Long, prefix: String) {
        val prefixList = getPrefixes(guildId).toMutableList()
        if (!prefixList.contains(prefix))
            prefixList.add(prefix)
        setPrefixes(guildId, prefixList)
    }

    private fun setPrefixes(guildId: Long, prefixList: List<String>) {
        val prefixes = prefixList.joinToString("%SPLIT%")
        guildPrefixDao.set(guildId, prefixes)
        guildPrefixDao.setCacheEntry(guildId, prefixList, NORMAL_CACHE)
    }

    suspend fun removePrefix(guildId: Long, prefix: String) {
        val prefixList = getPrefixes(guildId).toMutableList()
        prefixList.remove(prefix)
        setPrefixes(guildId, prefixList)
    }
}