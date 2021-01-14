package me.melijn.melijnbot.database.reminder

class ReminderWrapper(private val reminderDao: ReminderDao) {

    fun add(reminder: Reminder) {
        val (userId, remindAt, message) = reminder
        reminderDao.add(userId, remindAt, message)
    }

    fun remove(userId: Long, time: Long) {
        reminderDao.remove(userId, time)
    }

    suspend fun getRemindersOfUser(userId: Long): List<Reminder> {
        return reminderDao.getRemindersOfUser(userId)
    }

    suspend fun getReminders(cMillis: Long): List<Reminder> {
        return reminderDao.getPastReminders(cMillis)
    }
}