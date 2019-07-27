package me.melijn.melijnbot.commands.utility

import com.sun.management.OperatingSystemMXBean
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.Translateable
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
        syntax = Translateable("$root.syntax")
        aliases = arrayOf("statistics")
        description = Translateable("$root.description")
        commandCategory = CommandCategory.UTILITY
    }


    override fun execute(context: CommandContext) {
        val bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val totalMem = bean.totalPhysicalMemorySize shr 20
        val usedMem = totalMem - (bean.freePhysicalMemorySize shr 20)
        val totalJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.max shr 20
        val usedJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used shr 20
        val shardManager = context.getJDA().shardManager
        val voiceChannels = shardManager?.shards?.stream()?.mapToLong { shard ->
            shard.voiceChannels.stream().filter { vc -> vc.members.contains(vc.guild.selfMember) }.count()
        }?.sum()
        val threadPoolExecutor = context.taskManager.getExecutorService() as ThreadPoolExecutor
        val scheduledExecutorService = context.taskManager.getScheduledExecutorService() as ThreadPoolExecutor

        val title1 = Translateable("$root.response.field1.title").string(context)
        val title2 = Translateable("$root.response.field2.title").string(context)
        val title3 = Translateable("$root.response.field3.title").string(context)

        val value1 = replaceValue1Vars(
                Translateable("$root.response.field1.value").string(context),
                shardManager?.shardsTotal ?: 0,
                shardManager?.userCache?.size() ?: 0,
                shardManager?.guildCache?.size() ?: 0,
                voiceChannels ?: 0,
                threadPoolExecutor.activeCount + scheduledExecutorService.activeCount + scheduledExecutorService.queue.size,
                getDurationString(ManagementFactory.getRuntimeMXBean().uptime)
        )

        val value2 = replaceValue2Vars(
                Translateable("$root.response.field2.value").string(context),
                bean.availableProcessors,
                "${usedMem}MB/${totalMem}MB",
                getDurationString(getSystemUptime())
        )

        val value3 = replaceValue3Vars(
                Translateable("$root.response.field3.value").string(context),
                DecimalFormat("###.###%").format(bean.processCpuLoad),
                "${usedJVMMem}MB/${totalJVMMem}MB",
                "${Thread.activeCount()}/${Thread.getAllStackTraces().size}"
        )

        val embed = Embedder(context)
                .setThumbnail(context.getJDA().selfUser.effectiveAvatarUrl)
                .addField(title1, value1, false)
                .addField(title2, value2, false)
                .addField(title3, value3, false)
                .build()

        sendEmbed(context, embed)
    }

    private fun replaceValue1Vars(value: String, shardCount: Int, userCount: Long, guildCount: Long, voiceChannels: Long, threadCount: Int, uptime: String): String {
        return value
                .replace("%shardCount%", shardCount.toString())
                .replace("%userCount%", userCount.toString())
                .replace("%guildCount%", guildCount.toString())
                .replace("%cVCCount%", voiceChannels.toString())
                .replace("%botThreadCount%", threadCount.toString())
                .replace("%botUptime%", uptime)
    }

    private fun replaceValue2Vars(value: String, coreCount: Int, ramUsage: String, uptime: String): String {
        return value
                .replace("%coreCount%", coreCount.toString())
                .replace("%ramUsage%", ramUsage)
                .replace("%systemUptime%", uptime)
    }

    private fun replaceValue3Vars(value: String, cpuUsage: String, ramUsage: String, threadCount: String): String {
        return value
                .replace("%jvmCPUUsage%", cpuUsage)
                .replace("%ramUsage%", ramUsage)
                .replace("%threadCount%", threadCount)
    }
}