package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.music.MusicNodeCommand
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent

class VoiceLeaveListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildVoiceLeaveEvent) {
            runBlocking {
                musicNodeSwitchCheck(container, event)
                VoiceUtil.channelUpdate(container, event.channelLeft)
            }
        }
    }

    private suspend fun musicNodeSwitchCheck(container: Container, event: GuildVoiceLeaveEvent) {
        val guildId = event.guild.idLong
        if (MusicNodeCommand.map.isEmpty()) return
        HashMap(MusicNodeCommand.map).forEach {
            if (it.value.millis < (System.currentTimeMillis() - 60_000L) && it.key != guildId) {
                MusicNodeCommand.map.remove(it.key)
            }
        }

        val nodeInfo = MusicNodeCommand.map[guildId] ?: return
        val channel = event.guild.getVoiceChannelById(nodeInfo.channelId)
        if (channel == null) {
            MusicNodeCommand.map.remove(guildId)
            return
        }

        val track = nodeInfo.track
        val pos = nodeInfo.position
        val trackManager = container.lavaManager.musicPlayerManager.getGuildMusicPlayer(event.guild).guildTrackManager

        val premium = container.daoManager.musicNodeWrapper.isPremium(guildId)
        container.lavaManager.tryToConnectToVCSilent(channel, premium)

        trackManager.iPlayer.playTrack(track)
        trackManager.iPlayer.seekTo(pos)

        MusicNodeCommand.map.remove(guildId)
    }
}