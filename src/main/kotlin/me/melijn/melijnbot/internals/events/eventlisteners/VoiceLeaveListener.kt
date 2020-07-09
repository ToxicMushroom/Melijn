package me.melijn.melijnbot.internals.events.eventlisteners

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.music.MusicNodeCommand
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent

class VoiceLeaveListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) = runBlocking {
        if (event is GuildVoiceLeaveEvent) {
            musicNodeSwitchCheck(container, event)
            if (!event.member.user.isBot) {
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
        trackManager.iPlayer.volume = nodeInfo.volume
        try {
            trackManager.iPlayer.seekTo(pos)
        } catch (t: Throwable) {
            // might be a live stream
        }
        MusicNodeCommand.map.remove(guildId)
    }
}