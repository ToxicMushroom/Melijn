package me.melijn.melijnbot.database.supporters

import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.function.Consumer

class SupporterWrapper(val taskManager: TaskManager, private val userSupporterDao: UserSupporterDao) {

    var supporters: Set<Supporter> = setOf()
    var supporterIds: Set<Long> = setOf()

    init {
        taskManager.asyncAfter(Runnable {
            userSupporterDao.getSupporters(Consumer {
                supporters = it
                supporterIds = it.map { supporter -> supporter.userId }.toSet()
            })
        }, 2000)
    }
}