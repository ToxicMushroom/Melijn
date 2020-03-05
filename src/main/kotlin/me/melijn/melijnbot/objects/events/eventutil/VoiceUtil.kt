package me.melijn.melijnbot.objects.events.eventutil

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyMusicChannel
import me.melijn.melijnbot.objects.utils.listeningMembers
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.sharding.ShardManager

object VoiceUtil {

    //guildId, timeOfLeave
    var disconnectQueue = mutableMapOf<Long, Long>()

    suspend fun channelUpdate(container: Container, channelJoined: VoiceChannel) {
        val guild = channelJoined.guild
        val botChannel = container.lavaManager.getConnectedChannel(guild)
        val daoManager = container.daoManager

        // Leave channel timer stuff
        botChannel?.let {
            checkShouldDisconnectAndApply(it, daoManager)
        }


        // Radio stuff
        val musicChannel = guild.getAndVerifyMusicChannel(daoManager, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
            ?: return

        val musicUrl = daoManager.streamUrlWrapper.streamUrlCache.get(guild.idLong).await()
        if (musicUrl.isBlank()) return

        val musicPlayerManager = container.lavaManager.musicPlayerManager
        val trackManager = musicPlayerManager.getGuildMusicPlayer(guild).guildTrackManager
        val audioLoader = musicPlayerManager.audioLoader
        val iPlayer = trackManager.iPlayer

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

    suspend fun checkShouldDisconnectAndApply(botChannel: VoiceChannel, daoManager: DaoManager) {
        val guild = botChannel.guild
        if (
            !disconnectQueue.containsKey(guild.idLong) &&
            listeningMembers(botChannel) == 0 &&
            !(daoManager.music247Wrapper.music247Cache.get(guild.idLong).await() &&
                daoManager.supporterWrapper.guildSupporterIds.contains(guild.idLong))
        ) {
            disconnectQueue[guild.idLong] = System.currentTimeMillis() + 6_000
        } else {
            disconnectQueue.remove(guild.idLong)
        }
    }

    fun getConnectedChannelsAmount(shardManager: ShardManager, andHasListeners: Boolean = false): Long {
        return shardManager.shards.stream().mapToLong { shard ->
            getConnectedChannelsAmount(shard, andHasListeners)
        }?.sum() ?: 0
    }

    fun getConnectedChannelsAmount(inShard: JDA, andHasListeners: Boolean = false): Long {
        return inShard.voiceChannels.stream().filter { vc ->
            val contains = vc.members.contains(vc.guild.selfMember)
            val lm = listeningMembers(vc)
            if (andHasListeners) {
                contains && lm > 0
            } else {
                contains
            }
        }.count()
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