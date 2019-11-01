package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import lavalink.client.player.IPlayer
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.command.CommandContext


class GuildMusicPlayer(lavaManager: LavaManager, val guildId: Long) {

    val guildTrackManager: GuildTrackManager
    private val iPlayer: IPlayer = lavaManager.getIPlayer(guildId)

    init {
        guildTrackManager = GuildTrackManager(iPlayer)
        iPlayer.addListener(guildTrackManager)
    }

    fun getSendHandler(): AudioPlayerSendHandler = AudioPlayerSendHandler(iPlayer)
    fun safeQueueSilent(daoManager: DaoManager, track: AudioTrack): Boolean {
        if (daoManager.supporterWrapper.guildSupporterIds.contains(guildId) ||
            guildTrackManager.tracks.size + 1 <= QUEUE_LIMIT) {
            guildTrackManager.queue(track)
            return true
        }
        return false
    }

    fun safeQueue(context: CommandContext, track: AudioTrack): Boolean {
        val success = safeQueueSilent(context.daoManager, track)
        if (!success) {
            val msg =
        }

        return success
    }
}