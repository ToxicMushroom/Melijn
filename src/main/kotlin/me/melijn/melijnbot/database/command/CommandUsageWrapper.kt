package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.objects.threading.TaskManager

class CommandUsageWrapper(private val taskManager: TaskManager, private val commandUsageDao: CommandUsageDao) {

    suspend fun addUse(commandId: Int) {
        commandUsageDao.addUse(commandId)
    }

}