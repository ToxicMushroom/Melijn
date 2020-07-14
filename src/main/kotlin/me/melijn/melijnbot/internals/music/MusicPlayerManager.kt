package me.melijn.melijnbot.internals.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.music.lavaimpl.MelijnAudioPlayerManager
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap


class MusicPlayerManager(
    private val daoManager: DaoManager,
    private val lavaManager: LavaManager
) {

    private val logger = LoggerFactory.getLogger(MusicPlayerManager::class.java)
    val audioPlayerManager: MelijnAudioPlayerManager = MelijnAudioPlayerManager()
    val audioLoader = AudioLoader(this)

    companion object {
        val guildMusicPlayers: ConcurrentHashMap<Long, GuildMusicPlayer> = ConcurrentHashMap()
    }


    init {
        audioPlayerManager.configuration.isFilterHotSwapEnabled = true
        audioPlayerManager.frameBufferDuration = 1000
        AudioSourceManagers.registerRemoteSources(audioPlayerManager)
        AudioSourceManagers.registerLocalSource(audioPlayerManager)

    }

    fun getLPPlayer(): AudioPlayer {
        return audioPlayerManager.createPlayer()
    }

    fun getGuildMusicPlayer(guild: Guild): GuildMusicPlayer {
        val cachedMusicPlayer = guildMusicPlayers[guild.idLong]
        if (cachedMusicPlayer == null) {
            val newMusicPlayer = GuildMusicPlayer(daoManager, lavaManager, guild.idLong, "normal")

            guildMusicPlayers[guild.idLong] = newMusicPlayer
            logger.debug("new player for ${guild.id}")

            if (!lavaManager.lavalinkEnabled) {
                guild.audioManager.sendingHandler = newMusicPlayer.getSendHandler()
            }

            return newMusicPlayer
        }
        return cachedMusicPlayer
    }

    fun getPlayers() = guildMusicPlayers
}