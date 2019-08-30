package me.melijn.melijnbot

import com.fasterxml.jackson.databind.ObjectMapper
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.services.ServiceManager
import me.melijn.melijnbot.objects.threading.TaskManager
import java.io.File


class Container {
    companion object {
        lateinit var instance: Container
    }

    init {
        instance = this
    }

    var shuttingDown: Boolean = false
        set(value) {
            if (value) serviceManager.stopServices()
            field = value
        }
    var settings: Settings = ObjectMapper().readValue(File("config.json"), Settings::class.java)

    val taskManager = TaskManager()
    val daoManager = DaoManager(taskManager, settings.mySQL)
    val serviceManager = ServiceManager(daoManager)

    //messageId, reason
    val filteredMap = mutableMapOf<Long, String>()

    //messageId, purgerId
    val purgedIds = mutableMapOf<Long, Long>()

    //messageId
    val botDeletedMessageIds = mutableSetOf<Long>()
}
