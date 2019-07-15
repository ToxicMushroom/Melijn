package me.melijn.melijnbot

import me.duncte123.botcommons.config.ConfigUtils
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.MessageUtils


class Container {
    var settings: Settings = ConfigUtils.loadFromFile("config.json", Settings::class.java)
    val messageUtils = MessageUtils()
    val taskManager = TaskManager(messageUtils)
    val daoManager = DaoManager(taskManager, settings.mySQL)
}
