package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.threading.TaskManager

class CommandWrapper(private val taskManager: TaskManager, private val commandDao: CommandDao) {
    suspend fun insert(command: AbstractCommand) {
        taskManager.async {
            commandDao.insert(command)
        }
    }

    fun clearCommands() {
        taskManager.async {
            commandDao.clear()
        }
    }
}