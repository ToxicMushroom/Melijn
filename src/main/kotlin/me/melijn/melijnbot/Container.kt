package me.melijn.melijnbot

import com.fasterxml.jackson.databind.ObjectMapper
import lavalink.client.io.jda.JdaLavalink
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.music.LavaManager
import me.melijn.melijnbot.objects.services.ServiceManager
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.web.WebManager
import net.dv8tion.jda.api.sharding.ShardManager
import java.io.File
import java.net.URI


class Container {

    lateinit var shardManager: ShardManager
    var shuttingDown: Boolean = false
        set(value) {
            if (value) serviceManager.stopServices()
            field = value
        }

    var settings: Settings = ObjectMapper().readValue(File("config.json"), Settings::class.java)
    val taskManager = TaskManager()

    //Used by events
    val daoManager = DaoManager(taskManager, settings.database)
    val webManager = WebManager(taskManager, settings)
    //enabled on event
    val serviceManager = ServiceManager(taskManager, daoManager)

    lateinit var lavaManager: LavaManager

    var commandMap = emptyMap<Int, AbstractCommand>()
    //messageId, reason
    val filteredMap = mutableMapOf<Long, String>()
    //messageId, purgerId
    val purgedIds = mutableMapOf<Long, Long>()
    //messageId
    val botDeletedMessageIds = mutableSetOf<Long>()

    init {
        instance = this
    }

    companion object {
        lateinit var instance: Container
    }


    fun start(shardManager: ShardManager) {
        this.shardManager = shardManager

        val jdaLavaLink = if (settings.lavalink.enabled) {
            val linkBuilder = JdaLavalink(
                settings.id.toString(),
                settings.shardCount
            ) { id ->
                shardManager.getShardById(id)
            }

            for (node in settings.lavalink.nodes) {
                linkBuilder.addNode(URI.create(node.host), node.host)
            }
            linkBuilder
        } else {
            null
        }
        lavaManager = LavaManager(settings.lavalink.enabled, daoManager, shardManager, jdaLavaLink)
    }

}
