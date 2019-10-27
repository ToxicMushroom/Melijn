package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import net.dv8tion.jda.api.entities.Guild


class MusicPlayerManager(
    private val lavaManager: LavaManager
) {
    val audioPlayerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    val audioLoader = AudioLoader(this)
    private val guildMusicPlayers: HashMap<Long, GuildMusicPlayer> = HashMap()

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
            val newMusicPlayer = GuildMusicPlayer(lavaManager, guild.idLong)
            guildMusicPlayers[guild.idLong] = newMusicPlayer
            guild.audioManager.sendingHandler = newMusicPlayer.getSendHandler()
            return newMusicPlayer
        }
        return cachedMusicPlayer
    }
}