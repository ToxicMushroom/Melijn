package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE

class SelfRoleModeWrapper(private val selfRoleModeDao: SelfRoleModeDao) {


    suspend fun getMap(guildId: Long): SelfRoleMode {
        val result = selfRoleModeDao.getCacheEntry(guildId, HIGHER_CACHE)?.let { SelfRoleMode.valueOf(it) }

        if (result == null) {
            val selfroleMode = SelfRoleMode.valueOf(selfRoleModeDao.getMode(guildId))
            selfRoleModeDao.setCacheEntry(guildId, selfroleMode, NORMAL_CACHE)
            return selfroleMode
        }

        return result
    }

    fun set(guildId: Long, mode: SelfRoleMode) {
        if (mode == SelfRoleMode.AUTO) {
            selfRoleModeDao.delete(guildId)
        } else {
            selfRoleModeDao.setMode(guildId, mode.toString())
        }

        selfRoleModeDao.setCacheEntry(guildId, mode, NORMAL_CACHE)
    }
}