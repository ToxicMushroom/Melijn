package me.melijn.melijnbot.objects.services.voice

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil
import me.melijn.melijnbot.objects.services.Service
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class VoiceService(val container: Container, val shardManager: ShardManager) : Service("voice") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    private val statService = Runnable {
        runBlocking {
            val currentTime = System.currentTimeMillis()
            val disconnect = ArrayList(VoiceUtil.disconnectQueue.entries)
                .filter { (_, time) -> time < currentTime }
                .map { it.key }

            for (guildId in disconnect) {
                val guildMPlayer = container.lavaManager.musicPlayerManager.guildMusicPlayers.getOrElse(guildId) { null }
                guildMPlayer?.guildTrackManager?.stopAndDestroy()
                VoiceUtil.disconnectQueue.remove(guildId)
            }
        }
    }

    override fun start() {
        logger.info("Started VoiceService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(statService, 1, 1, TimeUnit.MINUTES)
    }

    override fun stop() {
        logger.info("Stopping VoiceService")
        scheduledFuture?.cancel(false)
    }
}