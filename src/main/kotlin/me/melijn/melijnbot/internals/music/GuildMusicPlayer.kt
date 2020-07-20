package me.melijn.melijnbot.internals.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.commands.music.NextSongPosition
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.isPremiumGuild
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable


class GuildMusicPlayer(daoManager: DaoManager, lavaManager: LavaManager, val guildId: Long, groupId: String) {

    val guildTrackManager: GuildTrackManager = GuildTrackManager(
        guildId,
        daoManager,
        lavaManager,
        lavaManager.getIPlayer(guildId, groupId),
        groupId
    )

    var groupId: String
        get() = guildTrackManager.groupId
        set(value) {
            guildTrackManager.groupId = value
        }

    init {
        this.groupId = groupId
    }

    val searchMenus: MutableMap<Long, TracksForQueue> = mutableMapOf()

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
    suspend fun safeQueueSilent(daoManager: DaoManager, track: AudioTrack, nextPos: NextSongPosition): Boolean {
        if (
            (guildTrackManager.trackSize() <= DONATE_QUEUE_LIMIT && isPremiumGuild(daoManager, guildId)) ||
            guildTrackManager.tracks.size + 1 <= QUEUE_LIMIT
        ) {
            guildTrackManager.queue(track, nextPos)
            return true
        }
        return false
    }

    suspend fun safeQueue(context: CommandContext, track: AudioTrack, nextPos: NextSongPosition): Boolean {
        val success = safeQueueSilent(context.daoManager, track, nextPos)
        if (!success) {
            TaskManager.async {
                val msg = context.getTranslation("message.music.queuelimit")
                    .withVariable("amount", QUEUE_LIMIT.toString())
                    .withVariable("donateAmount", DONATE_QUEUE_LIMIT.toString())
                sendRsp(context, msg)
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
                TaskManager.async {
                    val msg = context.getTranslation("message.music.queuelimit")
                        .withVariable("amount", QUEUE_LIMIT.toString())
                        .withVariable("donateAmount", DONATE_QUEUE_LIMIT.toString())
                    sendRsp(context, msg)
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