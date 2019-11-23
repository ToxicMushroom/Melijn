package me.melijn.melijnbot

import com.fasterxml.jackson.databind.ObjectMapper
import lavalink.client.io.jda.JdaLavalink
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.music.LavaManager
import me.melijn.melijnbot.objects.services.ServiceManager
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.web.RestServer
import me.melijn.melijnbot.objects.web.WebManager
import net.dv8tion.jda.api.sharding.ShardManager
import java.io.File


class Container {

    var restServer: RestServer? = null
    var shuttingDown: Boolean = false
        set(value) {
            if (value) serviceManager.stopServices()
            field = value
        }

    var startTime = System.currentTimeMillis()

    var settings: Settings = ObjectMapper().readValue(File("config.json"), Settings::class.java)
    val taskManager = TaskManager()

    //Used by events
    val daoManager = DaoManager(taskManager, settings.database)
    val webManager = WebManager(taskManager, settings)
    //enabled on event
    val serviceManager = ServiceManager(taskManager, daoManager, webManager)

    lateinit var lavaManager: LavaManager

    var commandMap = emptyMap<Int, AbstractCommand>()
    //messageId, reason
    val filteredMap = mutableMapOf<Long, String>()
    //messageId, purgerId
    val purgedIds = mutableMapOf<Long, Long>()
    //messageId
    val botDeletedMessageIds = mutableSetOf<Long>()

    var jdaLavaLink: JdaLavalink? = null

    init {
        instance = this
    }

    companion object {
        lateinit var instance: Container
    }

    fun initShardManager(shardManager: ShardManager) {
        lavaManager = LavaManager(settings.lavalink.enabled, daoManager, shardManager, jdaLavaLink)
    }


    fun initLava(jdaLavaLink: JdaLavalink?) {
        this.jdaLavaLink = jdaLavaLink
    }

    val uptimeMillis: Long
        get() = System.currentTimeMillis() - startTime
}
