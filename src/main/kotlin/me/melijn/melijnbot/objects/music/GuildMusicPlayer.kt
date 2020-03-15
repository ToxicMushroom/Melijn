package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.runBlocking
import lavalink.client.player.IPlayer
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.isPremiumGuild
import me.melijn.melijnbot.objects.utils.sendMsg


class GuildMusicPlayer(daoManager: DaoManager, lavaManager: LavaManager, val guildId: Long) {

    val searchMenus: MutableMap<Long, List<AudioTrack>> = mutableMapOf()
    private val iPlayer: IPlayer = lavaManager.getIPlayer(guildId, runBlocking { daoManager.musicNodeWrapper.isPremium(guildId) })
    val guildTrackManager: GuildTrackManager = GuildTrackManager(guildId, daoManager, lavaManager, iPlayer)

    init {
        iPlayer.addListener(guildTrackManager)
    }

    fun destroyTrackManager() {
        iPlayer.removeListener(guildTrackManager)
    }

    fun getSendHandler(): AudioPlayerSendHandler = AudioPlayerSendHandler(iPlayer)
    fun safeQueueSilent(daoManager: DaoManager, track: AudioTrack): Boolean {
        if (
            (guildTrackManager.trackSize() <= DONATE_QUEUE_LIMIT && isPremiumGuild(daoManager, guildId)) ||
            guildTrackManager.tracks.size + 1 <= QUEUE_LIMIT
        ) {
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
        if (
            guildTrackManager.tracks.size + add > QUEUE_LIMIT ||
            (guildTrackManager.tracks.size + add > DONATE_QUEUE_LIMIT && isPremiumGuild(context))
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