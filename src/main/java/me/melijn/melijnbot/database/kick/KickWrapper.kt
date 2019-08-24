package me.melijn.melijnbot.database.kick

import me.melijn.melijnbot.objects.threading.TaskManager

class KickWrapper(val taskManager: TaskManager, private val kickDao: KickDao) {

    fun addKick(kick: Kick) {
        kickDao.add(kick)
    }
}