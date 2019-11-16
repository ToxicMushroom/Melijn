package me.melijn.melijnbot.objects.events.eventutil

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyMusicChannel
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.VoiceChannel

object VoiceUtil {

    suspend fun channelUpdate(container: Container, channelJoined: VoiceChannel) {
        val guild = channelJoined.guild
        val daoManager = container.daoManager

        val musicChannel = guild.getAndVerifyMusicChannel(daoManager, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
            ?: return

        val musicUrl = container.daoManager.streamUrlWrapper.streamUrlCache.get(guild.idLong).await()
        if (musicUrl == "") return


        val selfMember = guild.selfMember
        val musicPlayerManager = container.lavaManager.musicPlayerManager
        val trackManager = musicPlayerManager.getGuildMusicPlayer(guild).guildTrackManager
        val audioLoader = musicPlayerManager.audioLoader
        val iPlayer = trackManager.iPlayer
        val botChannel = container.lavaManager.getConnectedChannel(guild)

        if (musicChannel.id == botChannel?.id && channelJoined.id == botChannel.id && iPlayer.playingTrack != null) {
            return
        } else if (musicChannel.id == botChannel?.id && channelJoined.id == botChannel.id) {
            audioLoader.loadNewTrack(daoManager, container.lavaManager, channelJoined, guild.jda.selfUser, musicUrl)
        } else if (botChannel == null && musicChannel.id == channelJoined.id) {
            if (container.lavaManager.tryToConnectToVCSilent(musicChannel)) {
                audioLoader.loadNewTrack(daoManager, container.lavaManager, channelJoined, guild.jda.selfUser, musicUrl)
            }
        }
    }
}