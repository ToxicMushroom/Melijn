package me.melijn.melijnbot.database.commands

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.threading.TaskManager

class CommandWrapper(private val taskManager: TaskManager, private val commandDao: CommandDao) {
    fun insert(command: AbstractCommand) {
        taskManager.async(Runnable {
            commandDao.insert(command)
        })
    }

    fun clearCommands() {
        taskManager.async(Runnable {
            commandDao.clear()
        })
    }
}