package me.melijn.melijnbot.objects.services.voice

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil
import me.melijn.melijnbot.objects.music.MusicPlayerManager
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.Task
import me.melijn.melijnbot.objects.utils.listeningMembers
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class VoiceService(val container: Container, val shardManager: ShardManager) : Service("voice") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    private val voiceService = Task {
        val currentTime = System.currentTimeMillis()
        val disconnect = ArrayList(VoiceUtil.disconnectQueue.entries)
            .filter { (_, time) -> time < currentTime }
            .map { it.key }

        for (guildId in disconnect) {
            val guild = shardManager.getGuildById(guildId) ?: continue
            val guildMPlayer = MusicPlayerManager.guildMusicPlayers.getOrElse(guildId) {
                null
            }

            val connectedChannel = guild.selfMember.voiceState?.channel
            if (connectedChannel != null && listeningMembers(connectedChannel) > 0) {
                VoiceUtil.disconnectQueue.remove(guildId)
                continue
            }

            if (guildMPlayer?.guildTrackManager != null) {
                guildMPlayer.guildTrackManager.stopAndDestroy()
                MusicPlayerManager.guildMusicPlayers.remove(guildMPlayer.guildId)

            } else {
                val isPremium = container.daoManager.musicNodeWrapper.isPremium(guildId)
                container.lavaManager.closeConnection(guildId, isPremium)
            }

            VoiceUtil.disconnectQueue.remove(guildId)
        }
    }

    override fun start() {
        logger.info("Started VoiceService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(voiceService, 1, 1, TimeUnit.MINUTES)
    }

    override fun stop() {
        logger.info("Stopping VoiceService")
        scheduledFuture?.cancel(false)
    }
}