package me.melijn.melijnbot.internals.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.threading.TaskManager
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.StatusChangeEvent

class BotStartShutdownListener(container: Container) : AbstractListener(container) {

    override suspend fun onEvent(event: GenericEvent) {
        if (event is StatusChangeEvent) {
            onStatusChange(event)
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
            event.jda.emotes.forEach {
                container.daoManager.emoteCache.save(it)
            }

            if (readyShards != PodInfo.shardsPerPod) return

            if (!container.serviceManager.slowStarted) {
                shardManager.setStatus(OnlineStatus.ONLINE)
                shardManager.setActivity(Activity.listening("commands | ${container.settings.botInfo.prefix}help"))
                TaskManager.async {
                    logger.info("Starting music clients..")
                    logger.info("Started music clients")
                }
                TaskManager.async {
                    container.serviceManager.startSlowServices()
                    logger.info("Slow Services ready")
                }
            }
        }
    }
}