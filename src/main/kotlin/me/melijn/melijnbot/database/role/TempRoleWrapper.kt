package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.internals.threading.TaskManager

class TempRoleWrapper(val taskManager: TaskManager, private val tempRoleDao: TempRoleDao) {

    suspend fun addTempRole(guildId: Long, userId: Long, roleId: Long, duration: Long, added: Boolean) {
        val start = System.currentTimeMillis()
        tempRoleDao.set(guildId, userId, roleId, start, start + duration, added)
    }

    suspend fun removeTempRole(userId: Long, roleId: Long) {
        tempRoleDao.remove(userId, roleId)
    }

    suspend fun getObjects(): List<TempRoleInfo> {
        return tempRoleDao.getFinishedObjects(System.currentTimeMillis())
    }
}