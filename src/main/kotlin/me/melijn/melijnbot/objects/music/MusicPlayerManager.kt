package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.services.voice.VOICE_SAFE
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.LoggerFactory


class MusicPlayerManager(
    private val daoManager: DaoManager,
    private val lavaManager: LavaManager
) {

    private val logger = LoggerFactory.getLogger(MusicPlayerManager::class.java)
    val audioPlayerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    val audioLoader = AudioLoader(this)

    companion object {
        val guildMusicPlayers: HashMap<Long, GuildMusicPlayer> = HashMap()
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

    @Synchronized
    fun getGuildMusicPlayer(guild: Guild): GuildMusicPlayer {
        val cachedMusicPlayer = guildMusicPlayers[guild.idLong]
        if (cachedMusicPlayer == null) {
            val newMusicPlayer = GuildMusicPlayer(daoManager, lavaManager, guild.idLong)
            VOICE_SAFE.submit {
                guildMusicPlayers[guild.idLong] = newMusicPlayer
                logger.debug("new player for ${guild.id}")
            }

            if (!lavaManager.lavalinkEnabled) {
                guild.audioManager.sendingHandler = newMusicPlayer.getSendHandler()
            }

            return newMusicPlayer
        }
        return cachedMusicPlayer
    }

    fun getPlayers() = guildMusicPlayers
}