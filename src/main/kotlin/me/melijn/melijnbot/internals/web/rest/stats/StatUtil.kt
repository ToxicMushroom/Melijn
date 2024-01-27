package me.melijn.melijnbot.internals.web.rest.stats

import com.sun.management.OperatingSystemMXBean
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.JvmUsage
import me.melijn.melijnbot.internals.utils.getSystemUptime
import me.melijn.melijnbot.internals.web.RequestContext
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import java.lang.management.ManagementFactory

fun computeBaseObject(): DataObject {
    val bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
    val (totalMem, usedMem, totalJVMMem, usedJVMMem) = JvmUsage.current(bean)

    val dataObject = DataObject.empty()
    dataObject.put(
        "bot", DataObject.empty()
            .put("uptime", ManagementFactory.getRuntimeMXBean().uptime)
            .put("melijnThreads", Thread.activeCount())
            .put("ramUsage", usedJVMMem)
            .put("ramTotal", totalJVMMem)
            .put("jvmThreads", Thread.getAllStackTraces().size)
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


fun computeShardStatsObject(
    jda: JDA,
) = DataObject.empty()
    .put("guildCount", jda.guildCache.size())
    .put("userCount", jda.userCache.size())
    .put("ping", jda.gatewayPing)
    .put("status", jda.status)
    .put("responses", jda.responseTotal)
    .put("id", jda.shardInfo.shardId)
    .put("unavailable", jda.guilds.count { it.jda.isUnavailable(it.idLong) })

fun computePublicStatsObject(context: RequestContext): DataArray {
    val shardManager = MelijnBot.shardManager
    val dataArray = DataArray.empty()

    for (shard in shardManager.shardCache) {
        dataArray.add(computeShardStatsObject(shard))
    }
    return dataArray
}
