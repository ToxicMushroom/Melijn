package me.melijn.melijnbot.internals.services.voice

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil.checkShouldDisconnectAndApply
import me.melijn.melijnbot.internals.music.MusicPlayerManager
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

class VoiceScoutService(
    val container: Container,
    val shardManager: ShardManager
) : Service("VoiceScout", 30, 30, TimeUnit.SECONDS) {

    override val service = RunnableTask {

        val gmp = MusicPlayerManager.guildMusicPlayers
        val iterator = gmp.iterator()

        while (iterator.hasNext()) {
            val guildMusicPlayer = iterator.next().value
            val guild = shardManager.getGuildById(guildMusicPlayer.guildId)
            if (guild == null) {
                guildMusicPlayer.guildTrackManager.stopAndDestroy(false)
                guildMusicPlayer.removeTrackManagerListener()
                iterator.remove()
            } else {
                val botChannel = container.lavaManager.getConnectedChannel(guild)
                val daoManager = container.daoManager

                // Leave channel timer stuff
                botChannel?.let {
                    checkShouldDisconnectAndApply(it, daoManager)
                } ?: run {
                    guildMusicPlayer.guildTrackManager.stopAndDestroy(false)
                    guildMusicPlayer.removeTrackManagerListener()
                    iterator.remove()
                }
            }

            val iter = VoiceUtil.disconnectQueue.iterator()
            while (iter.hasNext()) {
                val it = iter.next()
                if (it.value.isDone) iter.remove()
            }
        }
    }
}