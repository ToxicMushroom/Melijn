package me.melijn.melijnbot.database.role

class TempRoleWrapper(private val tempRoleDao: TempRoleDao) {

    fun addTempRole(guildId: Long, userId: Long, roleId: Long, duration: Long, added: Boolean) {
        val start = System.currentTimeMillis()
        tempRoleDao.set(guildId, userId, roleId, start, start + duration, added)
    }

    fun removeTempRole(userId: Long, roleId: Long) {
        tempRoleDao.remove(userId, roleId)
    }

    suspend fun getObjects(): List<TempRoleInfo> {
        return tempRoleDao.getFinishedObjects(System.currentTimeMillis())
    }
}