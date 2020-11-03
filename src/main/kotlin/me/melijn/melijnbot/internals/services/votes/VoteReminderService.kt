package me.melijn.melijnbot.internals.services.votes

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.LogUtils
import java.util.concurrent.TimeUnit

class VoteReminderService(val daoManager: DaoManager) : Service("VoteReminder", 2, 0, TimeUnit.MINUTES) {

    override val service: RunnableTask = RunnableTask {
        val cMillis = System.currentTimeMillis() + 120_000
        val voteReminderWrapper = daoManager.voteReminderWrapper
        val votes = voteReminderWrapper.getReminders(cMillis)
        val denyVoteWrapper = daoManager.denyVoteReminderWrapper

        val usersWithDeniedReminders = mutableListOf<Long>()
        for ((userId, remindAt) in votes) {
            val denied = denyVoteWrapper.contains(userId)
            if (denied) {
                usersWithDeniedReminders.add(userId)
                continue
            }
            TaskManager.asyncAfter(remindAt - System.currentTimeMillis()) {
                LogUtils.sendVoteReminder(daoManager, userId)
                voteReminderWrapper.removeReminder(userId)
            }


        }
        voteReminderWrapper.bulkRemove(usersWithDeniedReminders)
    }
}