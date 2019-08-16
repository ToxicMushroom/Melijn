package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.objects.threading.TaskManager

class BanWrapper(val taskManager: TaskManager, private val banDao: BanDao) {

    fun getUnbannableBans(): List<Ban> {
        return banDao.getUnbannableBans()
    }
}