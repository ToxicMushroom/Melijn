package me.melijn.melijnbot.internals.services.reminders

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.LogUtils
import java.util.concurrent.TimeUnit

class ReminderService(val daoManager: DaoManager) : Service("Reminder", 10, 10, TimeUnit.SECONDS) {

    override val service: RunnableTask = RunnableTask {
        val millis = System.currentTimeMillis()
        val cMillis = millis + 10_000
        val voteReminderWrapper = daoManager.reminderWrapper
        val votes = voteReminderWrapper.getReminders(cMillis)

        for ((userId, remindAt, message) in votes) {
            TaskManager.asyncAfter(remindAt - millis) {
                LogUtils.sendReminder(daoManager, userId, remindAt, message)
                voteReminderWrapper.remove(userId, remindAt)
            }
        }
    }
}