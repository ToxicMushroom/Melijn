package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.internals.command.AbstractCommand
import java.util.*

class CommandWrapper(private val commandDao: CommandDao) {

    fun clearCommands() {
        commandDao.clear()
    }

    suspend fun bulkInsert(commands: HashSet<AbstractCommand>) {
        commandDao.bulkInsert(commands)
    }
}