package me.melijn.melijnbot.objects.music

import lavalink.client.player.IPlayer


class GuildMusicPlayer(lavaManager: LavaManager, val guildId: Long) {

    val guildTrackManager: GuildTrackManager
    private val iPlayer: IPlayer = lavaManager.getIPlayer(guildId)

    init {
        guildTrackManager = GuildTrackManager(iPlayer)
        iPlayer.addListener(guildTrackManager)
    }

    fun getSendHandler(): AudioPlayerSendHandler = AudioPlayerSendHandler(iPlayer)
}