package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.objects.threading.TaskManager

class UnverifiedUsersWrapper(val taskManager: TaskManager, private val unverifiedUsersDao: UnverifiedUsersDao) {

    suspend fun getRecaptchaCode(guildId: Long, userId: Long): Long {
        return unverifiedUsersDao.getCode(guildId, userId)
    }

    suspend fun remove(guildId: Long, userId: Long) {
        unverifiedUsersDao.remove(guildId, userId)
    }

    suspend fun add(guildId: Long, userId: Long) {
        unverifiedUsersDao.add(guildId, userId)
    }
}