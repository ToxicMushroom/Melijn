package me.melijn.melijnbot.objects.events.eventutil

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyMusicChannel
import me.melijn.melijnbot.objects.utils.listeningMembers
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.sharding.ShardManager

object VoiceUtil {

    suspend fun channelUpdate(container: Container, channelJoined: VoiceChannel) {
        val guild = channelJoined.guild
        val daoManager = container.daoManager

        val musicChannel = guild.getAndVerifyMusicChannel(daoManager, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
            ?: return

        val musicUrl = daoManager.streamUrlWrapper.streamUrlCache.get(guild.idLong).await()
        if (musicUrl == "") return


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

            val premium = daoManager.musicNodeWrapper.isPremium(guild.idLong)
            if (container.lavaManager.tryToConnectToVCSilent(musicChannel, premium)) {
                audioLoader.loadNewTrack(daoManager, container.lavaManager, channelJoined, guild.jda.selfUser, musicUrl)
            }
        }
    }

    fun getConnectedChannelsAmount(shardManager: ShardManager, andHasListeners: Boolean = false): Long {
        return shardManager.shards.stream().mapToLong { shard ->
            shard.voiceChannels.stream().filter { vc ->
                val contains = vc.members.contains(vc.guild.selfMember)
                val lm = listeningMembers(vc)
                if (andHasListeners) {
                    contains && lm > 0
                } else {
                    contains
                }
            }.count()
        }?.sum() ?: 0
    }

    suspend fun resumeMusic(event: StatusChangeEvent, container: Container) {
        val wrapper = container.daoManager.tracksWrapper
        val music = wrapper.getMap()
        val channelMap = wrapper.getChannels()
        val shardManager = event.jda.shardManager ?: return
        val mpm = container.lavaManager.musicPlayerManager
        for ((guildId, tracks) in music) {
            val guild = shardManager.getGuildById(guildId) ?: continue
            val channel = channelMap[guildId]?.let { guild.getVoiceChannelById(it) } ?: continue

            val premium = container.daoManager.musicNodeWrapper.isPremium(guild.idLong)
            if (container.lavaManager.tryToConnectToVCSilent(channel, premium)) {
                val mp = mpm.getGuildMusicPlayer(guild)
                for (track in tracks) {
                    mp.safeQueueSilent(container.daoManager, track)
                }
            }
        }
        wrapper.clearChannels()
        wrapper.clear()
    }
}