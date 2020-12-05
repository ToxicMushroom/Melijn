package me.melijn.melijnbot.internals.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import kotlinx.coroutines.delay
import me.melijn.llklient.player.IPlayer
import me.melijn.llklient.player.event.AudioEventAdapterWrapped
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.commands.music.NextSongPosition
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.internals.threading.SafeList
import me.melijn.melijnbot.internals.threading.Task
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.LogUtils
import me.melijn.melijnbot.internals.utils.YTSearch
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.random.Random


class GuildTrackManager(
    val guildId: Long,
    val daoManager: DaoManager,
    val lavaManager: LavaManager,
    var iPlayer: IPlayer,
    var groupId: String
) : AudioEventAdapterWrapped() {

    var votedUsers = mutableListOf<Long>()

    val playingTrack: AudioTrack?
        get() = iPlayer.playingTrack

    private val logger = LoggerFactory.getLogger(this::class.java.name + " - $guildId")

    val resumeMomentMessageMap = mutableMapOf<Long, MessageEmbed>()
    val pauseMomentMessageMap = mutableMapOf<Long, MessageEmbed>()
    val startMomentMessageMap = mutableMapOf<Long, MessageEmbed>()


    var loopedTrack = false
    var loopedQueue = false

    var tracks: SafeList<AudioTrack> = SafeList()
    fun trackSize() = tracks.size


    private suspend fun nextTrack(lastTrack: AudioTrack) {
        votedUsers.clear()
        if (tracks.isEmpty()) {
            if (loopedQueue || loopedTrack) {
                chekNChangeGroup(lastTrack.info.uri)
                iPlayer.playTrack(lastTrack.makeClone())
                return
            }

            Task {
                lavaManager.closeConnection(guildId)
            }.run()

            return
        }

        if (loopedTrack) {
            chekNChangeGroup(lastTrack.info.uri)
            iPlayer.playTrack(lastTrack.makeClone())
            return
        }

        val track: AudioTrack = tracks.removeAtOrNull(0) ?: return
        chekNChangeGroup(track.info.uri)
        if (track == lastTrack) {
            iPlayer.playTrack(track.makeClone())
        } else {
            iPlayer.playTrack(track)
        }

        if (loopedQueue) {
            tracks.add(lastTrack)
        }
    }

    private suspend fun chekNChangeGroup(uri: String) {
        if (YTSearch.isUnknownHTTP(uri)) {
            if (groupId == "normal") {
                lavaManager.changeGroup(guildId, "http")
                groupId = "http"
            }
        } else if (groupId == "http") {
            lavaManager.changeGroup(guildId, "normal")
            groupId = "normal"
        }
    }

    /** returns the song postition **/
    suspend fun queue(track: AudioTrack, nextPos: NextSongPosition) {
        if (track.userData == null) throw IllegalArgumentException("no")
        if (iPlayer.playingTrack == null) {
            chekNChangeGroup(track.info.uri)
            iPlayer.playTrack(track)
        } else {
            when (nextPos) {
                NextSongPosition.BOTTOM -> {
                    tracks.add(track)
                }
                NextSongPosition.TOP -> {
                    tracks.add(0, track)
                }
                NextSongPosition.RANDOM -> {
                    val pos = Random.nextInt(tracks.size) + 1
                    tracks.add(pos, track)
                }
                NextSongPosition.TOPSKIP -> {
                    tracks.add(0, track)
                    skip(1)
                }
            }
        }
    }

    suspend fun shuffle() {
        tracks.shuffle()
    }


    override fun onPlayerResume(player: AudioPlayer?) {
        TaskManager.async {
            val data = playingTrack?.userData as TrackUserData? ?: return@async
            val embed = getResumedEmbedFromMap(data.currentTime) ?: return@async
            val guild = getAndCheckGuild() ?: return@async

            val channel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.MUSIC)
                ?: return@async
            sendEmbed(daoManager.embedDisabledWrapper, channel, embed)
        }
    }

    override fun onPlayerPause(player: AudioPlayer?) {
        TaskManager.async {
            val data = playingTrack?.userData as TrackUserData? ?: return@async
            val embed = getPausedEmbedFromMap(data.currentTime) ?: return@async
            val guild = getAndCheckGuild() ?: return@async

            val channel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.MUSIC)
                ?: return@async
            sendEmbed(daoManager.embedDisabledWrapper, channel, embed)
        }
    }

    override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack, thresholdMs: Long) {
        logger.debug("track stuck $thresholdMs")
    }

    override fun onTrackException(player: AudioPlayer?, track: AudioTrack, exception: FriendlyException) {
        TaskManager.async {
            val guild = getAndCheckGuild() ?: return@async
            LogUtils.sendMusicPlayerException(daoManager, guild, track, exception)
        }
    }

    override fun onTrackStart(player: AudioPlayer?, track: AudioTrack) {
        TaskManager.async {
            val data = track.userData as TrackUserData? ?: return@async
            val embed = getStartEmbedFromMap(data.currentTime) ?: return@async
            val guild = getAndCheckGuild() ?: return@async

            val channel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.MUSIC)
                ?: return@async
            sendEmbed(daoManager.embedDisabledWrapper, channel, embed)
        }
    }

    private suspend fun getStartEmbedFromMap(time: Long, nesting: Int = 0, nestCountLimit: Int = 5): MessageEmbed? {
        return startMomentMessageMap.getOrElse(time) {
            delay(500)
            return if (nesting >= nestCountLimit) {
                null
            } else {
                getStartEmbedFromMap(time, nesting + 1, nestCountLimit)
            }
        }
    }

    private suspend fun getPausedEmbedFromMap(time: Long, nesting: Int = 0, nestCountLimit: Int = 5): MessageEmbed? {
        return pauseMomentMessageMap.getOrElse(time) {
            delay(100)
            return if (nesting >= nestCountLimit) {
                null
            } else {
                getPausedEmbedFromMap(time, nesting + 1, nestCountLimit)
            }
        }
    }

    private suspend fun getResumedEmbedFromMap(time: Long, nesting: Int = 0, nestCountLimit: Int = 5): MessageEmbed? {
        return resumeMomentMessageMap.getOrElse(time) {
            delay(100)
            return if (nesting >= nestCountLimit) {
                null
            } else {
                getResumedEmbedFromMap(time, nesting + 1, nestCountLimit)
            }
        }
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack, endReason: AudioTrackEndReason) {
        //logger.debug("track ended eventStartNext:" + endReason.mayStartNext)

        if (endReason.mayStartNext) {
            TaskManager.async { nextTrack(track) }
        }
    }

    suspend fun clear() {
        tracks.clear()
    }

    suspend fun getPosition(audioTrack: AudioTrack): Int =
        if (iPlayer.playingTrack == audioTrack) {
            0
        } else {
            tracks.indexOf(audioTrack) + 1
        }

    //PLEASE RUN IN VOICE_SAFE
    suspend fun stopAndDestroy() {
        clear()
        iPlayer.stopTrack()

        Task {
            lavaManager.closeConnection(guildId)
        }.run()
    }


    suspend fun skip(amount: Int) {
        val nextTrack: AudioTrack? = tracks.removeFirstAndGetNextOrNull(amount)

        if (nextTrack == null) {
            stopAndDestroy()
        } else {
            iPlayer.stopTrack()
            chekNChangeGroup(nextTrack.info.uri)
            iPlayer.playTrack(nextTrack)
        }
    }

    suspend fun setPaused(paused: Boolean) {
        iPlayer.setPaused(paused)
    }

    suspend fun removeAt(indexes: IntArray): Map<Int, AudioTrack> {
        val removed = HashMap<Int, AudioTrack>()

        for (index in indexes.sortedBy { it }.reversed()) {
            removed[index] = tracks.removeAt(index)
        }

        return removed
    }

    private suspend fun getAndCheckGuild(): Guild? {
        val guild = MelijnBot.shardManager.getGuildById(guildId)
        if (guild == null) {
            stopAndDestroy()
        }
        return guild
    }
}