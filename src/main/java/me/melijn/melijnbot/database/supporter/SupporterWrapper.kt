package me.melijn.melijnbot.database.supporter

import me.melijn.melijnbot.objects.threading.TaskManager

class SupporterWrapper(val taskManager: TaskManager, private val userSupporterDao: UserSupporterDao) {

    var supporters: Set<Supporter> = setOf()
    var supporterIds: Set<Long> = setOf()

    init {
        taskManager.asyncAfter(2000) {
            userSupporterDao.getSupporters {
                supporters = it
                supporterIds = it.map { supporter -> supporter.userId }.toSet()
            }
        }
    }
}