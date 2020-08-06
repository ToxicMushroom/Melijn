package me.melijn.melijnbot.database.settings

class DenyVoteReminderWrapper(private val denyVoteReminderDao: DenyVoteReminderDao) {

    suspend fun contains(userId: Long): Boolean {
        return denyVoteReminderDao.contains(userId)
    }

    suspend fun add(userId: Long) {
        denyVoteReminderDao.add(userId)
    }

    suspend fun remove(userId: Long) {
        denyVoteReminderDao.delete(userId)
    }
}