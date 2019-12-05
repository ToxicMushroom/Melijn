package me.melijn.melijnbot.commands.utility

import com.sun.management.OperatingSystemMXBean
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil
import me.melijn.melijnbot.objects.utils.getDurationString
import me.melijn.melijnbot.objects.utils.getSystemUptime
import me.melijn.melijnbot.objects.utils.sendEmbed
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import java.util.concurrent.ThreadPoolExecutor


class StatsCommand : AbstractCommand("command.stats") {

    init {
        id = 4
        name = "stats"
        aliases = arrayOf("statistics")
        commandCategory = CommandCategory.UTILITY
    }


    override suspend fun execute(context: CommandContext) {
        val bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val totalMem = bean.totalPhysicalMemorySize shr 20
        val usedMem = totalMem - (bean.freePhysicalMemorySize shr 20)
        val totalJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.max shr 20
        val usedJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used shr 20
        val shardManager = context.shardManager
        val voiceChannels = VoiceUtil.getConnectedChannelsAmount(shardManager)
        val voiceChannelsNotEmpty = VoiceUtil.getConnectedChannelsAmount(shardManager, true)

        val threadPoolExecutor = context.taskManager.executorService as ThreadPoolExecutor
        val scheduledExecutorService = context.taskManager.scheduledExecutorService as ThreadPoolExecutor


        val title1 = context.getTranslation("$root.response.field1.title")
        val title2 = context.getTranslation("$root.response.field2.title")
        val title3 = context.getTranslation("$root.response.field3.title")

        val unReplaceField1 = context.getTranslation("$root.response.field1.value")
        val value1 = replaceValue1Vars(
            unReplaceField1,
            shardManager.shardsTotal,
            shardManager.userCache.size(),
            shardManager.guildCache.size(),
            voiceChannelsNotEmpty,
            voiceChannels,
            threadPoolExecutor.activeCount + scheduledExecutorService.activeCount + scheduledExecutorService.queue.size,
            getDurationString(ManagementFactory.getRuntimeMXBean().uptime)
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
            .setThumbnail(context.selfUser.effectiveAvatarUrl)
            .addField(title1, value1, false)
            .addField(title2, value2, false)
            .addField(title3, value3, false)
            .build()

        sendEmbed(context, embed)
    }


    private fun replaceValue1Vars(
        value: String,
        shardCount: Int,
        userCount: Long,
        guildCount: Long,
        voiceChannelsNotEmpty: Long,
        voiceChannels: Long,
        threadCount: Int,
        uptime: String
    ): String = value
        .replace("%shardCount%", shardCount.toString())
        .replace("%userCount%", userCount.toString())
        .replace("%guildCount%", guildCount.toString())
        .replace("%cVCCount%", "$voiceChannelsNotEmpty/$voiceChannels")
        .replace("%botThreadCount%", threadCount.toString())
        .replace("%botUptime%", uptime)


    private fun replaceValue2Vars(value: String, coreCount: Int, ramUsage: String, uptime: String): String = value
        .replace("%coreCount%", coreCount.toString())
        .replace("%ramUsage%", ramUsage)
        .replace("%systemUptime%", uptime)


    private fun replaceValue3Vars(value: String, cpuUsage: String, ramUsage: String, threadCount: String): String = value
        .replace("%jvmCPUUsage%", cpuUsage)
        .replace("%ramUsage%", ramUsage)
        .replace("%threadCount%", threadCount)

}