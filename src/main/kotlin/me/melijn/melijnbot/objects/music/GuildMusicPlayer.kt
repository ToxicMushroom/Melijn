package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import lavalink.client.player.IPlayer
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendMsg


class GuildMusicPlayer(daoManager: DaoManager, lavaManager: LavaManager, val guildId: Long) {

    val guildTrackManager: GuildTrackManager
    val searchMenus: MutableMap<Long, List<AudioTrack>> = mutableMapOf()
    private val iPlayer: IPlayer = lavaManager.getIPlayer(guildId)

    init {
        guildTrackManager = GuildTrackManager(guildId, daoManager, lavaManager, iPlayer)
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
            context.taskManager.async {
                val msg = context.getTranslation("message.music.queuelimit")
                    .replace("%amount%", QUEUE_LIMIT.toString())
                    .replace("%donateAmount%", DONATE_QUEUE_LIMIT.toString())
                sendMsg(context, msg)
            }
        }

        return success
    }

    fun queueIsFull(context: CommandContext, add: Int, silent: Boolean = false): Boolean {
        if ((guildTrackManager.tracks.size + add > QUEUE_LIMIT &&
                !context.daoManager.supporterWrapper.guildSupporterIds.contains(guildId)) ||
            guildTrackManager.tracks.size + add > DONATE_QUEUE_LIMIT
        ) {
            if (!silent) {
                context.taskManager.async {
                    val msg = context.getTranslation("message.music.queuelimit")
                        .replace("%amount%", QUEUE_LIMIT.toString())
                        .replace("%donateAmount%", DONATE_QUEUE_LIMIT.toString())
                    sendMsg(context, msg)
                }
            }
            return true
        }
        return false
    }
}