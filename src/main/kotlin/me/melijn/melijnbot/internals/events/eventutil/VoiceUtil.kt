package me.melijn.melijnbot.internals.events.eventutil

import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.withPermit
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.music.NextSongPosition
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.services.voice.VOICE_SAFE
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyMusicChannel
import me.melijn.melijnbot.internals.utils.listeningMembers
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.sharding.ShardManager

object VoiceUtil {

    //guildId, timeOfLeave
    var disconnectQueue = mutableMapOf<Long, Long>()

    suspend fun channelUpdate(container: Container, channelUpdate: VoiceChannel) {
        val guild = channelUpdate.guild
        val botChannel = container.lavaManager.getConnectedChannel(guild)
        val daoManager = container.daoManager

        // Leave channel timer stuff
        botChannel?.let {
            VOICE_SAFE.withPermit {
                checkShouldDisconnectAndApply(it, daoManager)
            }
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

        if (musicChannel.id == botChannel?.id && channelUpdate.id == botChannel.id && iPlayer.playingTrack != null) {
            return
        } else if (musicChannel.id == botChannel?.id && channelUpdate.id == botChannel.id) {
            audioLoader.loadNewTrack(daoManager, container.lavaManager, channelUpdate, guild.jda.selfUser, musicUrl, NextSongPosition.BOTTOM)

        } else if (botChannel == null && musicChannel.id == channelUpdate.id) {
            if (listeningMembers(musicChannel, container.settings.id) > 0) {
                val premium = daoManager.musicNodeWrapper.isPremium(guild.idLong)
                if (container.lavaManager.tryToConnectToVCSilent(musicChannel, premium)) {
                    audioLoader.loadNewTrack(daoManager, container.lavaManager, channelUpdate, guild.jda.selfUser, musicUrl, NextSongPosition.BOTTOM)
                }
            }
        }
    }

    suspend fun checkShouldDisconnectAndApply(botChannel: VoiceChannel, daoManager: DaoManager) {
        val guildId = botChannel.guild.idLong
        if (
            listeningMembers(botChannel) == 0 &&
            !(daoManager.music247Wrapper.music247Cache.get(guildId).await() &&
                daoManager.supporterWrapper.guildSupporterIds.contains(guildId))
        ) {
            if (!disconnectQueue.containsKey(guildId)) {
                disconnectQueue[guildId] = System.currentTimeMillis() + 600_000
            }
        } else {
            disconnectQueue.remove(guildId)
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
                    mp.safeQueueSilent(container.daoManager, track, NextSongPosition.BOTTOM)
                }
            }
        }
        wrapper.clearChannels()
        wrapper.clear()
    }
}