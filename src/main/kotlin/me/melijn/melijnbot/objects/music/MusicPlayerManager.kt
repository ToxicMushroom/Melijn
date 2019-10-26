package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import me.melijn.melijnbot.objects.utils.YTSearch
import net.dv8tion.jda.api.entities.Guild


class MusicPlayerManager(
    private val lavaManager: LavaManager
) {
    private val playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val players: HashMap<Long, GuildMusicPlayer> = HashMap()
    private val ytSearch: YTSearch = YTSearch()


    init {
        playerManager.configuration.isFilterHotSwapEnabled = true
        playerManager.frameBufferDuration = 1000
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    fun getLPPlayer(): AudioPlayer {
        return playerManager.createPlayer()
    }

    @Synchronized
    fun getPlayer(guild: Guild): GuildMusicPlayer = getPlayer(guild.idLong)

    @Synchronized
    fun getPlayer(guildId: Long): GuildMusicPlayer  {
        var guildMusicPlayer = players[guildId]
        if (guildMusicPlayer == null) {
            guildMusicPlayer = GuildMusicPlayer(lavaManager, guildId)
        }
        return guildMusicPlayer
    }
}