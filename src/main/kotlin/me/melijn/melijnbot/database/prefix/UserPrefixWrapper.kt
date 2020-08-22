package me.melijn.melijnbot.database.prefix

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.internals.utils.splitIETEL

class UserPrefixWrapper(private val userPrefixDao: UserPrefixDao) {

    suspend fun getPrefixes(userId: Long): List<String> {
        val result = userPrefixDao.getCacheEntry(userId, HIGHER_CACHE)?.splitIETEL("%SPLIT%")

        if (result != null) return result

        val prefixes = userPrefixDao.get(userId)
        userPrefixDao.setCacheEntry(userId, prefixes, NORMAL_CACHE)
        return prefixes.splitIETEL("%SPLIT%")
    }

    suspend fun addPrefix(userId: Long, prefix: String) {
        val prefixList = getPrefixes(userId).toMutableList()
        if (!prefixList.contains(prefix))
            prefixList.add(prefix)
        setPrefixes(userId, prefixList)
    }

    private fun setPrefixes(userId: Long, prefixList: List<String>) {
        val prefixes = prefixList.joinToString("%SPLIT%")
        userPrefixDao.set(userId, prefixes)
        userPrefixDao.setCacheEntry(userId, prefixes, NORMAL_CACHE)
    }

    suspend fun removePrefix(userId: Long, prefix: String) {
        val prefixList = getPrefixes(userId).toMutableList()
        prefixList.remove(prefix)
        setPrefixes(userId, prefixList)
    }
}