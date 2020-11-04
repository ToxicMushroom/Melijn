package me.melijn.melijnbot.database.reminder

import me.melijn.melijnbot.database.votes.VoteReminder

class ReminderWrapper(val reminderDao: ReminderDao) {
    fun add(reminder: VoteReminder) {

    }

    fun remove(userId: Long, time: Long) {
        reminderDao.remove(userId, time)
    }
}