package me.melijn.jda.commands.util;

import com.sun.management.OperatingSystemMXBean;
import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;

import javax.management.*;
import java.lang.management.ManagementFactory;

import static me.melijn.jda.Melijn.PREFIX;

public class StatsCommand extends Command {

    public StatsCommand() {
        this.commandName = "stats";
        this.description = "Shows server stats";
        this.usage = PREFIX + commandName;
        this.category = Category.UTILS;
    }

    @Override
    protected void execute(CommandEvent event) {
        OperatingSystemMXBean bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long totalMem = bean.getTotalPhysicalMemorySize() >> 20;
        long usedMem = totalMem - (bean.getFreePhysicalMemorySize() >> 20);
        long totalJVMMem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax() >> 20;
        long usedJVMMem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() >> 20;
        int voiceChannels = 0;
        for (Guild guild : event.getJDA().asBot().getShardManager().getGuilds()) {
            if (guild.getAudioManager().isConnected() || guild.getAudioManager().isAttemptingToConnect()) voiceChannels++;
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
                        "\n**Uptime** " + Helpers.getOnlineTime() +
                        "\n\u200B", false)
                .addField("Server Stats", "" +
                        "\n**CPU Usage** " + getProcessCpuLoad() + "%" +
                        "\n**RAM Usage** " + usedMem + "MB/" + totalMem + "MB" +
                        "\n\u200B", false)
                .addField("JVM Stats", "" +
                        "\n**RAM Usage** " + usedJVMMem + "MB/" + totalJVMMem + "MB" +
                        "\n**Threads** " + Thread.activeCount(), false)
                .build());
    }

    private static double getProcessCpuLoad() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name;
        try {
            name = ObjectName.getInstance("java.lang:type=OperatingSystem");
            AttributeList list;
            list = mbs.getAttributes(name, new String[]{"ProcessCpuLoad"});
            if (list.isEmpty()) return Double.NaN;

            Attribute att = (Attribute) list.get(0);
            double value = (double) att.getValue();

            // usually takes a couple of seconds before we get real values
            if (value == -1.0) return Double.NaN;
            // returns a percentage value with 1 decimal point precision
            return ((int) (value * 1000) / 10.0);
        } catch (InstanceNotFoundException | ReflectionException | MalformedObjectNameException e) {
            e.printStackTrace();
            return Double.NaN;
        }


    }
}
