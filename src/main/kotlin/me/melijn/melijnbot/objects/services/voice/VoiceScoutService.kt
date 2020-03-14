package me.melijn.melijnbot.objects.services.voice

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil.checkShouldDisconnectAndApply
import me.melijn.melijnbot.objects.music.MusicPlayerManager
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.Task
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class VoiceScoutService(val container: Container, val shardManager: ShardManager) : Service("voicescout") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    private val voiceScoutService = Task {
        val gmp = MusicPlayerManager.guildMusicPlayers
        ArrayList(gmp.values).forEach { guildMusicPlayer ->
            val guild = shardManager.getGuildById(guildMusicPlayer.guildId)
            if (guild == null) {
                guildMusicPlayer.guildTrackManager.clear()
                guildMusicPlayer.guildTrackManager.iPlayer.stopTrack()
                MusicPlayerManager.guildMusicPlayers.remove(guildMusicPlayer.guildId)
            } else {
                val botChannel = container.lavaManager.getConnectedChannel(guild)
                val daoManager = container.daoManager

                // Leave channel timer stuff
                botChannel?.let {
                    checkShouldDisconnectAndApply(it, daoManager)
                }

                if (botChannel == null) {
                    guildMusicPlayer.guildTrackManager.clear()
                    guildMusicPlayer.guildTrackManager.iPlayer.stopTrack()
                    MusicPlayerManager.guildMusicPlayers.remove(guildMusicPlayer.guildId)
                }
            }
        }
    }

    override fun start() {
        logger.info("Started VoiceScoutService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(voiceScoutService, 10, 10, TimeUnit.MINUTES)
    }

    override fun stop() {
        logger.info("Stopping VoiceScoutService")
        scheduledFuture?.cancel(false)
    }
}