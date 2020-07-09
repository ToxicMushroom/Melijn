package me.melijn.melijnbot.internals.services.voice

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import me.melijn.melijnbot.internals.music.MusicPlayerManager
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.utils.listeningMembers
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

val VOICE_SAFE = Semaphore(1, 0)

class VoiceService(
    val container: Container,
    val shardManager: ShardManager
) : Service("Voice", 1, 1, TimeUnit.MINUTES) {

    override val service = RunnableTask {
        VOICE_SAFE.withPermit {
            val currentTime = System.currentTimeMillis()
            val disconnect = ArrayList(VoiceUtil.disconnectQueue.entries)
                .filter { (_, time) -> time < currentTime }
                .map { it.key }

            for (guildId in disconnect) {
                val guild = shardManager.getGuildById(guildId)
                if (guild == null) {
                    VoiceUtil.disconnectQueue.remove(guildId)
                    //logger.info("$guildId guild null")
                    continue
                }

                val guildMPlayer = MusicPlayerManager.guildMusicPlayers.getOrElse(guildId) {
                    null
                }

                val connectedChannel = guild.selfMember.voiceState?.channel
                if (connectedChannel != null && listeningMembers(connectedChannel) > 0) {
                    VoiceUtil.disconnectQueue.remove(guildId)
                    //logger.info("$guildId someone is listening")
                    continue
                }

                if (guildMPlayer?.guildTrackManager != null) {
                    guildMPlayer.guildTrackManager.stopAndDestroy()
                    //logger.info("$guildId stopped player & left")

                } else {
                    val isPremium = container.daoManager.musicNodeWrapper.isPremium(guildId)
                    container.lavaManager.closeConnection(guildId, isPremium)
                    //logger.info("$guildId left")
                }

                VoiceUtil.disconnectQueue.remove(guildId)
            }
        }
    }
}