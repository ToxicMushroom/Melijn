package me.melijn.melijnbot.database

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.internals.threading.TaskManager


class TestDaoManager(taskManager: TaskManager, dbSettings: Settings.Database) {

    companion object {
        val afterTableFunctions = mutableListOf<() -> Unit>()
    }

    var dbVersion: String
    var connectorVersion: String

    var driverManager: DriverManager

    init {
        driverManager = DriverManager(dbSettings
            //, dbSettings.mySQL
        )

        runBlocking {
            dbVersion = driverManager.getDBVersion()
            connectorVersion = driverManager.getConnectorVersion()
        }

        //After registering wrappers
        driverManager.executeTableRegistration()
        for (func in afterTableFunctions) {
            func()
        }
    }
}