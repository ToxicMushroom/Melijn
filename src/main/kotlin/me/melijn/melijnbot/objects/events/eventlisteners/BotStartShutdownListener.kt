package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil
import me.melijn.melijnbot.objects.web.RestServer
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.StatusChangeEvent
import org.jooby.Jooby

class BotStartShutdownListener(container: Container) : AbstractListener(container) {


    override fun onEvent(event: GenericEvent) {
        if (event is StatusChangeEvent) {
            runBlocking { onStatusChange(event) }
        }
    }

    private suspend fun onStatusChange(event: StatusChangeEvent) {
        val shardManager = event.jda.shardManager ?: return
        if (event.newStatus == JDA.Status.CONNECTED) {
            val readyShards = shardManager.shards.count { jda -> jda.status == JDA.Status.CONNECTED }
            val loadedAllShards = readyShards == container.settings.shardCount

            if (!loadedAllShards) {
                logger.info("$readyShards shard(s) ready")
                return
            }

            logger.info("All shards ready")
            if (!container.serviceManager.started) {
                container.taskManager.async {
                    logger.info("Starting music clients..")
                    VoiceUtil.resumeMusic(event, container)
                    logger.info("Started music clients")
                }
                container.startTime = System.currentTimeMillis()
                logger.info("Starting services..")
                container.serviceManager.init(container, shardManager)
                container.serviceManager.startServices()
                logger.info("Services ready")
                logger.info("Starting Jooby rest server..")
                val restServer = RestServer(container)
                container.taskManager.async {
                    Jooby.run({ restServer }, arrayOf("application.port=${container.settings.restPort}"))
                }
                container.restServer = restServer
                logger.info("Started Jooby rest server")
            }
        }
    }
}