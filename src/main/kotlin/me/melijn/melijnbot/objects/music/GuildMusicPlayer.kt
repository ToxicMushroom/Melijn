package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.commands.music.NextSongPosition
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.isPremiumGuild
import me.melijn.melijnbot.objects.utils.message.sendMsg
import me.melijn.melijnbot.objects.utils.withVariable


class GuildMusicPlayer(daoManager: DaoManager, lavaManager: LavaManager, val guildId: Long) {

    val searchMenus: MutableMap<Long, TracksForQueue> = mutableMapOf()
    val guildTrackManager: GuildTrackManager = GuildTrackManager(guildId, daoManager, lavaManager, lavaManager.getIPlayer(guildId, runBlocking { daoManager.musicNodeWrapper.isPremium(guildId) }))

    init {
        addTrackManagerListener()
    }

    fun addTrackManagerListener() {
        guildTrackManager.iPlayer.addListener(guildTrackManager)
    }

    fun removeTrackManagerListener() {
        guildTrackManager.iPlayer.removeListener(guildTrackManager)
    }

    fun getSendHandler(): AudioPlayerSendHandler = AudioPlayerSendHandler(guildTrackManager.iPlayer)
    fun safeQueueSilent(daoManager: DaoManager, track: AudioTrack, nextPos: NextSongPosition): Boolean {
        if (
            (guildTrackManager.trackSize() <= DONATE_QUEUE_LIMIT && isPremiumGuild(daoManager, guildId)) ||
            guildTrackManager.tracks.size + 1 <= QUEUE_LIMIT
        ) {
            guildTrackManager.queue(track, nextPos)
            return true
        }
        return false
    }

    fun safeQueue(context: CommandContext, track: AudioTrack, nextPos: NextSongPosition): Boolean {
        val success = safeQueueSilent(context.daoManager, track, nextPos)
        if (!success) {
            context.taskManager.async {
                val msg = context.getTranslation("message.music.queuelimit")
                    .withVariable("amount", QUEUE_LIMIT.toString())
                    .withVariable("donateAmount", DONATE_QUEUE_LIMIT.toString())
                sendMsg(context, msg)
            }
        }

        return success
    }

    fun queueIsFull(context: CommandContext, add: Int, silent: Boolean = false): Boolean {
        if (
            guildTrackManager.tracks.size + add > QUEUE_LIMIT ||
            (guildTrackManager.tracks.size + add > DONATE_QUEUE_LIMIT && isPremiumGuild(context))
        ) {
            if (!silent) {
                context.taskManager.async {
                    val msg = context.getTranslation("message.music.queuelimit")
                        .withVariable("amount", QUEUE_LIMIT.toString())
                        .withVariable("donateAmount", DONATE_QUEUE_LIMIT.toString())
                    sendMsg(context, msg)
                }
            }
            return true
        }
        return false
    }
}

data class TracksForQueue(
    val audioTracks: List<AudioTrack>,
    val nextPosition: NextSongPosition
)