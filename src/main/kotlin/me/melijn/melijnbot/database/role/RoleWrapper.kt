package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.RoleType

class RoleWrapper(private val roleDao: RoleDao) {

    suspend fun getRoleId(guildId: Long, roleType: RoleType): Long {
        val result = roleDao.getCacheEntry("$roleType:$guildId", HIGHER_CACHE)?.toLong()

        if (result != null) return result

        val roleId = roleDao.get(guildId, roleType)
        roleDao.setCacheEntry("$roleType:$guildId", roleId, NORMAL_CACHE)
        return roleId
    }

    fun removeRole(guildId: Long, roleType: RoleType) {
        roleDao.unset(guildId, roleType)
        roleDao.setCacheEntry("$roleType:$guildId", -1, NORMAL_CACHE)
    }

    fun setRole(guildId: Long, roleType: RoleType, roleId: Long) {
        roleDao.set(guildId, roleType, roleId)
        roleDao.setCacheEntry("$roleType:$guildId", roleId, NORMAL_CACHE)
    }

    //guildId -> roleId
    suspend fun getRoles(birthday: RoleType): Map<Long, Long> {
        return roleDao.getRoles(birthday)
    }
}