package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import me.melijn.melijnbot.database.DaoManager
import net.dv8tion.jda.api.entities.Guild


class MusicPlayerManager(
    private val daoManager: DaoManager,
    private val lavaManager: LavaManager
) {

    val audioPlayerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    val audioLoader = AudioLoader(this)
    val guildMusicPlayers: HashMap<Long, GuildMusicPlayer> = HashMap()

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
            guildMusicPlayers[guild.idLong] = newMusicPlayer
            if (!lavaManager.lavalinkEnabled) {
                guild.audioManager.sendingHandler = newMusicPlayer.getSendHandler()
            }

            return newMusicPlayer
        }
        return cachedMusicPlayer
    }

    fun getPlayers() = guildMusicPlayers
    fun setGuildNode(guildId: Long, nodeId: Int) {

    }
}