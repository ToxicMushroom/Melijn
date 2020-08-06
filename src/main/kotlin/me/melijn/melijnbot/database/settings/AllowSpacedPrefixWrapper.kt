package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.internals.models.TriState

class AllowSpacedPrefixWrapper(
    private val allowSpacedPrefixDao: AllowSpacedPrefixDao,
    private val privateAllowSpacedPrefixDao: PrivateAllowSpacedPrefixDao
) {

    suspend fun getGuildState(guildId: Long): Boolean {
        val result = allowSpacedPrefixDao.getCacheEntry("$guildId", HIGHER_CACHE)?.toBoolean()

        if (result == null) {
            val allowed = allowSpacedPrefixDao.contains(guildId)
            allowSpacedPrefixDao.setCacheEntry(guildId, allowed, NORMAL_CACHE)
            return allowed
        }
        return result
    }

    suspend fun getUserTriState(userId: Long): TriState {
        val result = privateAllowSpacedPrefixDao.getCacheEntry("$userId", HIGHER_CACHE)?.let {
            TriState.valueOf(it)
        }

        if (result == null) {
            val tristate = privateAllowSpacedPrefixDao.getState(userId)
            privateAllowSpacedPrefixDao.setCacheEntry(userId, tristate, NORMAL_CACHE)
            return tristate
        }
        return result

    }

    fun setGuildState(guildId: Long, state: Boolean) {
        if (state) allowSpacedPrefixDao.add(guildId)
        else allowSpacedPrefixDao.delete(guildId)
        allowSpacedPrefixDao.setCacheEntry(guildId, state, NORMAL_CACHE)
    }

    fun setUserState(userId: Long, triState: TriState) {
        when (triState) {
            TriState.TRUE -> privateAllowSpacedPrefixDao.setState(userId, true)
            TriState.DEFAULT -> privateAllowSpacedPrefixDao.delete(userId)
            TriState.FALSE -> privateAllowSpacedPrefixDao.setState(userId, false)
        }
        privateAllowSpacedPrefixDao.setCacheEntry(userId, triState.toString(), NORMAL_CACHE)
    }
}