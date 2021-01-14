package me.melijn.melijnbot.database.verification

class UnverifiedUsersWrapper(private val unverifiedUsersDao: UnverifiedUsersDao) {

    suspend fun getMoment(guildId: Long, userId: Long): Long {
        return unverifiedUsersDao.getMoment(guildId, userId)
    }

    suspend fun getTries(guildId: Long, userId: Long): Long {
        return unverifiedUsersDao.getTries(guildId, userId)
    }

    fun remove(guildId: Long, userId: Long) {
        unverifiedUsersDao.remove(guildId, userId)
    }

    fun add(guildId: Long, userId: Long) {
        unverifiedUsersDao.add(guildId, userId)
    }

    suspend fun contains(guildId: Long, userId: Long): Boolean {
        return unverifiedUsersDao.contains(guildId, userId)
    }

    fun update(guildId: Long, userId: Long, tries: Long) {
        unverifiedUsersDao.update(guildId, userId, tries)
    }

    suspend fun getGuilds(userId: Long): List<Long> {
        return unverifiedUsersDao.getGuilds(userId)
    }
}