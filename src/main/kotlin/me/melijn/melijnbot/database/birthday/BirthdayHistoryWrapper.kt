package me.melijn.melijnbot.database.birthday

class BirthdayHistoryWrapper(private val birthdayHistoryDao: BirthdayHistoryDao) {

    suspend fun add(year: Int, guildId: Long, userId: Long) {
        birthdayHistoryDao.add(year, guildId, userId, System.currentTimeMillis())
    }

    suspend fun contains(year: Int, guildId: Long, userId: Long): Boolean {
        return birthdayHistoryDao.contains(year, guildId, userId)
    }

    suspend fun isActive(year: Int, guildId: Long, userId: Long): Boolean {
        return birthdayHistoryDao.isActive(year, guildId, userId)
    }

    suspend fun getBirthdaysToDeactivate(): Map<Long, Pair<Int, Long>> {
        return birthdayHistoryDao.getBirthdaysToRemove(System.currentTimeMillis() - 86_400_000)
    }

    suspend fun deactivate(year: Int, guildId: Long, userId: Long) {
        birthdayHistoryDao.deactivate(year, guildId, userId)
    }
}