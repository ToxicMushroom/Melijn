package me.melijn.melijnbot.database.votes

class VoteReminderWrapper(private val voteReminderDao: VoteReminderDao) {

    fun addReminder(userId: Long, remindAt: Long) {
        voteReminderDao.addReminder(userId, remindAt)
    }

    suspend fun getReminders(beforeMillis: Long): List<VoteReminder> {
        return voteReminderDao.getReminders(beforeMillis)
    }

    fun removeReminders(beforeMillis: Long) {
        voteReminderDao.removeReminders(beforeMillis)
    }

    fun removeReminder(userId: Long) {
        voteReminderDao.removeReminder(userId)
    }

    fun bulkRemove(userIds: MutableList<Long>) {
        voteReminderDao.removeReminders(userIds)
    }

    suspend fun getReminder(userId: Long): Long? {
        return voteReminderDao.get(userId)
    }

}