package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import lavalink.client.player.IPlayer


class GuildMusicPlayer(private val lavaManager: LavaManager, val guildId: Long) {

    private var manager: GuildTrackManager
    var iPlayer: IPlayer = lavaManager.getIPlayer(guildId)

    init {
        manager = GuildTrackManager(iPlayer, this)
        iPlayer.addListener(manager)
    }

    @Synchronized
    fun queue(track: AudioTrack?) {
        manager.queue(track)
    }

    @Synchronized
    fun resumeTrack() {
        iPlayer.isPaused = false
    }

    @Synchronized
    fun stopTrack() {
        iPlayer.stopTrack()
        lavaManager.closeConnection(guildId)
    }

    @Synchronized
    fun skipTrack() {
        iPlayer.stopTrack()
        manager.nextTrack(iPlayer.playingTrack)
    }

    @Synchronized
    fun isPaused(): Boolean {
        return iPlayer.isPaused
    }

}