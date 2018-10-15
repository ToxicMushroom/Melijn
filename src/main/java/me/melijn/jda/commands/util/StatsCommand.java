package me.melijn.jda.commands.util;

import com.sun.management.OperatingSystemMXBean;
import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;

import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;

import static me.melijn.jda.Melijn.PREFIX;

public class StatsCommand extends Command {

    public StatsCommand() {
        this.commandName = "stats";
        this.description = "Shows server stats";
        this.usage = PREFIX + commandName;
        this.category = Category.UTILS;
    }

    /* CREDITS TO DUNCTE123 FOR MOST OF THESE STATS AND DESIGN */

    @Override
    protected void execute(CommandEvent event) {
        OperatingSystemMXBean bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long totalMem = bean.getTotalPhysicalMemorySize() >> 20;
        long usedMem = totalMem - (bean.getFreePhysicalMemorySize() >> 20);
        long totalJVMMem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax() >> 20;
        long usedJVMMem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() >> 20;
        int voiceChannels = 0;
        for (Guild guild : event.getJDA().asBot().getShardManager().getGuilds()) {
            if (guild.getAudioManager().isConnected() || guild.getAudioManager().isAttemptingToConnect())
                voiceChannels++;
        }
        ShardManager shardManager = event.getJDA().asBot().getShardManager();
        event.reply(new EmbedBuilder()
                .setColor(Helpers.EmbedColor)
                .setThumbnail(event.getJDA().getSelfUser().getAvatarUrl())
                .addField("Bot stats", "" +
                        "\n**Shards** " + shardManager.getShardsTotal() +
                        "\n**Unique users** " + shardManager.getUserCache().size() +
                        "\n**Guilds** " + shardManager.getGuildCache().size() +
                        "\n**Connected VoiceChannels** " + voiceChannels +
                        "\n**Uptime** " + Helpers.getDurationBreakdown(ManagementFactory.getRuntimeMXBean().getUptime()) +
                        "\n\u200B", false)
                .addField("Server Stats", "" +
                        "\n**CPU Usage** " + new DecimalFormat("###.###%").format(bean.getProcessCpuLoad()) +
                        "\n**Cores** " + bean.getAvailableProcessors() +
                        "\n**RAM Usage** " + usedMem + "MB/" + totalMem + "MB" +
                        "\n\u200B", false)
                .addField("JVM Stats", "" +
                        "\n**RAM Usage** " + usedJVMMem + "MB/" + totalJVMMem + "MB" +
                        "\n**Threads** " + Thread.activeCount() + "/" + Thread.getAllStackTraces().size(), false)
                .build());
    }
}
