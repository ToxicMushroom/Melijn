package me.melijn.melijnbot.database.settings

class DenyVoteReminderWrapper(private val denyVoteReminderDao: DenyVoteReminderDao) {

    suspend fun contains(userId: Long): Boolean {
        return denyVoteReminderDao.contains(userId)
    }

    fun add(userId: Long) {
        denyVoteReminderDao.add(userId)
    }

    fun remove(userId: Long) {
        denyVoteReminderDao.delete(userId)
    }
}