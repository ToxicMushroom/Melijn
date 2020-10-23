package me.melijn.melijnbot.internals.web.rest.stats

import com.sun.management.OperatingSystemMXBean
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.events.EventManager
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.threading.TaskManager.scheduledExecutorService
import me.melijn.melijnbot.internals.utils.OSValidator
import me.melijn.melijnbot.internals.utils.getSystemUptime
import me.melijn.melijnbot.internals.utils.getTotalMBUnixRam
import me.melijn.melijnbot.internals.utils.getUsedMBUnixRam
import me.melijn.melijnbot.internals.web.RequestContext
import me.melijn.melijnbot.internals.web.WebUtils.respondJson
import me.melijn.melijnbot.objectMapper
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import java.lang.management.ManagementFactory
import java.util.concurrent.ThreadPoolExecutor

object StatsResponseHandler {

    var lastRequest = System.currentTimeMillis()

    suspend fun handleStatsResponse(context: RequestContext) {
        val bean: OperatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val totalMem: Long
        val usedMem: Long
        if (OSValidator.isUnix) {
            totalMem = getTotalMBUnixRam()
            usedMem = getUsedMBUnixRam()
        } else {
            totalMem = bean.totalMemorySize shr 20
            usedMem = totalMem - (bean.freeSwapSpaceSize shr 20)
        }

        val totalJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.max shr 20
        val usedJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used shr 20
        val threadPoolExecutor = TaskManager.executorService as ThreadPoolExecutor
        scheduledExecutorService as ThreadPoolExecutor

        val dataObject = DataObject.empty()
        dataObject.put("bot", DataObject.empty()
            .put("uptime", ManagementFactory.getRuntimeMXBean().uptime)
            .put("melijnThreads", threadPoolExecutor.activeCount + scheduledExecutorService.activeCount + scheduledExecutorService.queue.size)
            .put("ramUsage", usedJVMMem)
            .put("ramTotal", totalJVMMem)
            .put("jvmThreads", Thread.activeCount())
            .put("cpuUsage", bean.processCpuLoad * 100)
        )

        dataObject.put("server", DataObject.empty()
            .put("uptime", getSystemUptime())
            .put("ramUsage", usedMem)
            .put("ramTotal", totalMem)
        )



        dataObject.put("events", objectMapper.writeValueAsString(EventManager.eventCountMap))
        dataObject.put("lastPoint", lastRequest)

        for (event in EventManager.eventCountMap.keys) {
            EventManager.eventCountMap[event] = 0
        }

        val shardManager = MelijnBot.shardManager
        val dataArray = DataArray.empty()
        val players = context.lavaManager.musicPlayerManager.getPlayers()


        for (shard in shardManager.shardCache) {
            var queuedTracks = 0
            var musicPlayers = 0


            for (player in players.values) {
                if (shard.guildCache.getElementById(player.guildId) != null) {
                    if (player.guildTrackManager.iPlayer.playingTrack != null) {
                        musicPlayers++
                    }
                    queuedTracks += player.guildTrackManager.trackSize()
                }
            }


            dataArray.add(
                DataObject.empty()
                    .put("guildCount", shard.guildCache.size())
                    .put("userCount", shard.userCache.size())
                    .put("connectedVoiceChannels", VoiceUtil.getConnectedChannelsAmount(shard))
                    .put("listeningVoiceChannels", VoiceUtil.getConnectedChannelsAmount(shard, true))
                    .put("ping", shard.gatewayPing)
                    .put("status", shard.status)
                    .put("queuedTracks", queuedTracks)
                    .put("musicPlayers", musicPlayers)
                    .put("responses", shard.responseTotal)
                    .put("id", shard.shardInfo.shardId)
                    .put("unavailable", shard.unavailableGuilds.size)
            )
        }

        dataObject.put("shards", dataArray)
        context.call.respondJson(dataObject)
        lastRequest = System.currentTimeMillis()
    }
}