package me.melijn.melijnbot.objects.services.voice

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil.checkShouldDisconnectAndApply
import me.melijn.melijnbot.objects.music.MusicPlayerManager
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.Task
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

class VoiceScoutService(
    val container: Container,
    val shardManager: ShardManager
) : Service("VoiceScout", 10, 5, TimeUnit.MINUTES) {

    override val service = Task {
        val gmp = MusicPlayerManager.guildMusicPlayers
        gmp.values.iterator().forEach { guildMusicPlayer ->
            val guild = shardManager.getGuildById(guildMusicPlayer.guildId)
            if (guild == null) {
                guildMusicPlayer.guildTrackManager.clear()
                guildMusicPlayer.guildTrackManager.iPlayer.stopTrack()
                guildMusicPlayer.removeTrackManagerListener()
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
                    guildMusicPlayer.removeTrackManagerListener()
                    MusicPlayerManager.guildMusicPlayers.remove(guildMusicPlayer.guildId)
                }
            }
        }
    }
}