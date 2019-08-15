package me.melijn.melijnbot

import com.fasterxml.jackson.databind.ObjectMapper
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.threading.TaskManager
import java.io.File


class Container {
    companion object {
        lateinit var instance: Container
    }
    init {
        instance = this
    }
    var settings: Settings = ObjectMapper().readValue(File("config.json"), Settings::class.java)
    val taskManager = TaskManager()
    val daoManager = DaoManager(taskManager, settings.mySQL)
}
