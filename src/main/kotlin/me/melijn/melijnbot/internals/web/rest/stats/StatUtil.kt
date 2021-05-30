package me.melijn.melijnbot.internals.web.rest.stats

import com.sun.management.OperatingSystemMXBean
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.JvmUsage
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import me.melijn.melijnbot.internals.music.GuildMusicPlayer
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.getSystemUptime
import me.melijn.melijnbot.internals.web.RequestContext
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import java.lang.management.ManagementFactory
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadPoolExecutor

fun computeBaseObject(): DataObject {
    val bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
    val (totalMem, usedMem, totalJVMMem, usedJVMMem) = JvmUsage.current(bean)

    val threadPoolExecutor = TaskManager.executorService as ForkJoinPool
    val scheduledExecutorService = TaskManager.scheduledExecutorService as ThreadPoolExecutor

    val dataObject = DataObject.empty()
    dataObject.put(
        "bot", DataObject.empty()
            .put("uptime", ManagementFactory.getRuntimeMXBean().uptime)
            .put(
                "melijnThreads",
                threadPoolExecutor.activeThreadCount + scheduledExecutorService.activeCount + scheduledExecutorService.queue.size
            )
            .put("ramUsage", usedJVMMem)
            .put("ramTotal", totalJVMMem)
            .put("jvmThreads", Thread.activeCount())
            .put("cpuUsage", bean.processCpuLoad * 100)
    )

    dataObject.put(
        "server", DataObject.empty()
            .put("uptime", getSystemUptime())
            .put("ramUsage", usedMem)
            .put("ramTotal", totalMem)
    )
    return dataObject
}

fun getPlayersAndQueuedTracks(
    jda: JDA,
    players: Map<Long, GuildMusicPlayer>
): Pair<Int, Int> {
    var queuedTracks = 0
    var musicPlayers = 0

    for (player in players.values) {
        if (jda.guildCache.getElementById(player.guildId) != null) {
            if (player.guildTrackManager.iPlayer.playingTrack != null) {
                musicPlayers++
            }
            queuedTracks += player.guildTrackManager.trackSize()
        }
    }
    return Pair(queuedTracks, musicPlayers)
}

fun computeShardStatsObject(
    jda: JDA,
    queuedTracks: Int,
    musicPlayers: Int
) = DataObject.empty()
    .put("guildCount", jda.guildCache.size())
    .put("userCount", jda.userCache.size())
    .put("connectedVoiceChannels", VoiceUtil.getConnectedChannelsAmount(jda))
    .put("listeningVoiceChannels", VoiceUtil.getConnectedChannelsAmount(jda, true))
    .put("ping", jda.gatewayPing)
    .put("status", jda.status)
    .put("queuedTracks", queuedTracks)
    .put("musicPlayers", musicPlayers)
    .put("responses", jda.responseTotal)
    .put("id", jda.shardInfo.shardId)
    .put("unavailable", jda.guilds.count { it.jda.isUnavailable(it.idLong) })

fun computePublicStatsObject(context: RequestContext): DataArray {
    val shardManager = MelijnBot.shardManager
    val dataArray = DataArray.empty()
    val players = context.lavaManager.musicPlayerManager.getPlayers()

    for (shard in shardManager.shardCache) {
        val (queuedTracks, musicPlayers) = getPlayersAndQueuedTracks(shard, players)
        dataArray.add(computeShardStatsObject(shard, queuedTracks, musicPlayers))
    }
    return dataArray
}
