package me.melijn.melijnbot.internals.events.eventlisteners

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import me.melijn.melijnbot.internals.threading.TaskManager
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.StatusChangeEvent

class BotStartShutdownListener(container: Container) : AbstractListener(container) {


    override fun onEvent(event: GenericEvent) {
        if (event is StatusChangeEvent) {
            runBlocking { onStatusChange(event) }
        }
    }

    private suspend fun onStatusChange(event: StatusChangeEvent) {
        val shardManager = event.jda.shardManager
        if (shardManager == null) {
            logger.info("please use sharding")
            return
        }

        if (event.newStatus == JDA.Status.CONNECTED) {
            val readyShards = shardManager.shards.count { jda -> jda.status == JDA.Status.CONNECTED }
            logger.info("$readyShards/${shardManager.shards.size} shards ready")

            if (readyShards != container.settings.shardCount) return



            if (!container.serviceManager.started) {
                TaskManager.async {
                    logger.info("Starting music clients..")
                    VoiceUtil.resumeMusic(event, container)
                    logger.info("Started music clients")
                }
            }
        }
    }
}