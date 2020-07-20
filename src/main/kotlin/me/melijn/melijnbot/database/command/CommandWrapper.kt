package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.internals.command.AbstractCommand

class CommandWrapper(private val commandDao: CommandDao) {

    suspend fun insert(command: AbstractCommand) {
        commandDao.insert(command)
    }

    fun clearCommands() {
        commandDao.clear()
    }
}