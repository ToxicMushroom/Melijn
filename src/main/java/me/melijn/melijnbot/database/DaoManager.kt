package me.melijn.melijnbot.database

import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.database.commands.CommandDao
import me.melijn.melijnbot.database.commands.CommandWrapper
import me.melijn.melijnbot.objects.threading.TaskManager

class DaoManager(taskManager: TaskManager, mysqlSettings: Settings.MySQL) {

    val commandWrapper: CommandWrapper


    init {
        val driverManager = DriverManager(mysqlSettings)
        commandWrapper = CommandWrapper(taskManager, CommandDao(driverManager))


        //After registering wrappers
        driverManager.executeTableRegistration()
    }
}