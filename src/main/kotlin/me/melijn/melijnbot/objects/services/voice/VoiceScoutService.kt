package me.melijn.melijnbot.objects.services.voice

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil.checkShouldDisconnectAndApply
import me.melijn.melijnbot.objects.services.Service
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class VoiceScoutService(val container: Container, val shardManager: ShardManager) : Service("voice") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    private val statService = Runnable {
        runBlocking {
            val gmp = container.lavaManager.musicPlayerManager.guildMusicPlayers
            gmp.values.forEach { guildMusicPlayer ->
                val guild = shardManager.getGuildById(guildMusicPlayer.guildId)
                if (guild == null) {

                    guildMusicPlayer.guildTrackManager.stopAndDestroy()
                    gmp.remove(guildMusicPlayer.guildId)
                    return@forEach
                }

                val botChannel = container.lavaManager.getConnectedChannel(guild)
                val daoManager = container.daoManager

                // Leave channel timer stuff
                botChannel?.let {
                    checkShouldDisconnectAndApply(it, daoManager)
                }
            }
        }
    }

    override fun start() {
        logger.info("Started VoiceScoutService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(statService, 1, 1, TimeUnit.MINUTES)
    }

    override fun stop() {
        logger.info("Stopping VoiceScoutService")
        scheduledFuture?.cancel(false)
    }
}