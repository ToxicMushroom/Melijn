package me.melijn.melijnbot.database.votes

class VoteReminderWrapper(private val voteReminderDao: VoteReminderDao) {

    fun addReminder(voteReminder: VoteReminder) {
        voteReminderDao.addReminder(voteReminder.userId, voteReminder.flag, voteReminder.remindAt)
    }

    suspend fun getReminders(beforeMillis: Long): Map<Long, Map<Int, Long>> {
        val map = mutableMapOf<Long, Map<Int, Long>>()

        for ((userId, flag, remindAt) in voteReminderDao.getReminders(beforeMillis)) {
            val map2 = map[userId]?.toMutableMap()
            if (map2 == null)
                map[userId] = mapOf(flag to remindAt)
            else {
                map[userId] = map2 + (flag to remindAt)
            }
        }
        return map
    }

    fun removeReminder(userId: Long, flag: Int) {
        voteReminderDao.removeReminder(userId, flag)
    }

    fun bulkRemove(userIds: Map<Long, List<Int>>) {
        voteReminderDao.removeReminders(userIds)
    }

    suspend fun getReminder(userId: Long): List<VoteReminder> {
        return voteReminderDao.get(userId)
    }

}