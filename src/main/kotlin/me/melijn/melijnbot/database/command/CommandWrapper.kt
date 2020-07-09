package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.threading.TaskManager

class CommandWrapper(private val taskManager: TaskManager, private val commandDao: CommandDao) {

    suspend fun insert(command: AbstractCommand) {
        commandDao.insert(command)
    }

    fun clearCommands() {
        commandDao.clear()
    }
}