package me.melijn.melijnbot.commands.utility

import com.sun.management.OperatingSystemMXBean
import me.melijn.melijnbot.internals.JvmUsage
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.getSystemUptime
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.withVariable
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadPoolExecutor

class StatsCommand : AbstractCommand("command.stats") {

    init {
        id = 4
        name = "stats"
        aliases = arrayOf("statistics")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val (totalMem, usedMem, totalJVMMem, usedJVMMem) = JvmUsage.current(bean)

        val shardManager = context.shardManager
        val voiceChannels = VoiceUtil.getConnectedChannelsAmount(shardManager)
        val voiceChannelsNotEmpty = VoiceUtil.getConnectedChannelsAmount(shardManager, true)


        val threadPoolExecutor = TaskManager.executorService as ForkJoinPool
        val scheduledExecutorService = TaskManager.scheduledExecutorService as ThreadPoolExecutor

        val title1 = context.getTranslation("$root.response.field1.title")
        val title2 = context.getTranslation("$root.response.field2.title")
        val title3 = context.getTranslation("$root.response.field3.title")

        val unReplaceField1 = context.getTranslation("$root.response.field1.value")
        val value1 = replaceValue1Vars(
            unReplaceField1,
            PodInfo.shardsPerPod,
            shardManager.userCache.size(),
            shardManager.guildCache.size(),
            voiceChannelsNotEmpty,
            voiceChannels,
            threadPoolExecutor.activeThreadCount + scheduledExecutorService.activeCount + scheduledExecutorService.queue.size,
            getDurationString(ManagementFactory.getRuntimeMXBean().uptime),
            0,
            0
        )

        val unReplaceField2 = context.getTranslation("$root.response.field2.value")
        val value2 = replaceValue2Vars(
            unReplaceField2,
            bean.availableProcessors,
            "${usedMem}MB/${totalMem}MB",
            getDurationString(getSystemUptime())
        )

        val unReplaceField3 = context.getTranslation("$root.response.field3.value")
        val value3 = replaceValue3Vars(
            unReplaceField3,
            DecimalFormat("###.###%").format(bean.processCpuLoad),
            "${usedJVMMem}MB/${totalJVMMem}MB",
            "${Thread.activeCount()}/${Thread.getAllStackTraces().size}"
        )

        val embed = Embedder(context)
            .setTitle("Stats of cluster #" + PodInfo.podId)
            .setThumbnail(context.selfUser.effectiveAvatarUrl)
            .setDescription(
                """
                |```INI
                |[${title1}]```$value1
                |
                |```INI
                |[${title2}]```$value2
                |                                
                |```INI
                |[${title3}]```$value3
            """.trimMargin()
            )
            .build()

        sendEmbedRsp(context, embed)
    }

    private fun replaceValue1Vars(
        value: String,
        shardCount: Int,
        userCount: Long,
        guildCount: Long,
        voiceChannelsNotEmpty: Long,
        voiceChannels: Long,
        threadCount: Int,
        uptime: String,
        queuedTracks: Int,
        musicPlayers: Int
    ): String = value
        .withVariable("shardCount", shardCount.toString())
        .withVariable("userCount", userCount.toString())
        .withVariable("serverCount", guildCount.toString())
        .withVariable("cVCCount", "$voiceChannelsNotEmpty/$voiceChannels")
        .withVariable("botThreadCount", threadCount.toString())
        .withVariable("botUptime", uptime)
        .withVariable("queuedTracks", "$queuedTracks")
        .withVariable("musicPlayers", "$musicPlayers")

    private fun replaceValue2Vars(value: String, coreCount: Int, ramUsage: String, uptime: String): String = value
        .withVariable("coreCount", coreCount.toString())
        .withVariable("ramUsage", ramUsage)
        .withVariable("systemUptime", uptime)

    private fun replaceValue3Vars(value: String, cpuUsage: String, ramUsage: String, threadCount: String): String =
        value
            .withVariable("jvmCPUUsage", cpuUsage)
            .withVariable("ramUsage", ramUsage)
            .withVariable("threadCount", threadCount)
}