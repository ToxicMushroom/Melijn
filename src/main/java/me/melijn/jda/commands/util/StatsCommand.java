package me.melijn.jda.commands.util;

import com.sun.management.OperatingSystemMXBean;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.bot.sharding.ShardManager;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.concurrent.ThreadPoolExecutor;

import static me.melijn.jda.Melijn.PREFIX;

public class StatsCommand extends Command {

    public StatsCommand() {
        this.commandName = "stats";
        this.description = "Shows the bot's server statistics";
        this.usage = PREFIX + commandName;
        this.category = Category.UTILS;
        this.id = 94;
    }

    /* CREDITS TO DUNCTE123 FOR SOME OF THESE STATS AND DESIGN */

    @Override
    protected void execute(CommandEvent event) {
        OperatingSystemMXBean bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long totalMem = bean.getTotalPhysicalMemorySize() >> 20;
        long usedMem = totalMem - (bean.getFreePhysicalMemorySize() >> 20);
        long totalJVMMem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax() >> 20;
        long usedJVMMem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() >> 20;
        long voiceChannels = event.getJDA().asBot().getShardManager().getShards().stream().mapToLong(
                (shard) -> shard.getVoiceChannels().stream().filter(
                        (vc) -> vc.getMembers().contains(vc.getGuild().getSelfMember())
                ).count()
        ).sum();
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) event.getClient().getMelijn().getTaskManager().getExecutorService();
        ThreadPoolExecutor scheduledExecutorService = (ThreadPoolExecutor) event.getClient().getMelijn().getTaskManager().getScheduledExecutorService();

        ShardManager shardManager = event.getJDA().asBot().getShardManager();
        event.reply(new Embedder(event.getVariables(), event.getGuild())
                .setThumbnail(event.getJDA().getSelfUser().getAvatarUrl())
                .addField("Bot stats", "" +
                        "\n**Shards** " + shardManager.getShardsTotal() +
                        "\n**Unique users** " + shardManager.getUserCache().size() +
                        "\n**Guilds** " + shardManager.getGuildCache().size() +
                        "\n**Connected VoiceChannels** " + voiceChannels +
                        "\n**Threads** " + (threadPoolExecutor.getActiveCount() + scheduledExecutorService.getActiveCount() + scheduledExecutorService.getQueue().size()) +
                        "\n**Uptime** " + event.getMessageHelper().getDurationBreakdown(ManagementFactory.getRuntimeMXBean().getUptime()) +
                        "\n\u200B", false)
                .addField("Server Stats", "" +
                        "\n**Cores** " + bean.getAvailableProcessors() +
                        "\n**RAM Usage** " + usedMem + "MB/" + totalMem + "MB" +
                        "\n**System Uptime** " + event.getMessageHelper().getDurationBreakdown(event.getHelpers().getSystemUptime()) +
                        "\n\u200B", false)
                .addField("JVM Stats", "" +
                        "\n**CPU Usage** " + new DecimalFormat("###.###%").format(bean.getProcessCpuLoad()) +
                        "\n**RAM Usage** " + usedJVMMem + "MB/" + totalJVMMem + "MB" +
                        "\n**Threads** " + Thread.activeCount() + "/" + Thread.getAllStackTraces().size(), false)
                .build());
    }
}
