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
        val voteStateWrapper = daoManager.voteReminderStatesWrapper

        val usersWithDeniedReminders = mutableMapOf<Long, List<Int>>()
        for ((userId, reminders) in votes) {
            val voteStates = voteStateWrapper.getRaw(userId)
            for ((flag, remindAt) in reminders) {
                if (voteStates[flag] != true) {
                    usersWithDeniedReminders[userId] = (usersWithDeniedReminders[userId] ?: emptyList()) + flag
                    continue
                }

                TaskManager.asyncAfter(remindAt - System.currentTimeMillis()) {
                    LogUtils.sendVoteReminder(daoManager, flag, userId)
                    voteReminderWrapper.removeReminder(userId, flag)
                }
            }
        }
        voteReminderWrapper.bulkRemove(usersWithDeniedReminders)
    }
}