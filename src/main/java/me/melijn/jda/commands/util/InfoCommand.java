package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import com.sun.management.OperatingSystemMXBean;
import me.duncte123.weebJava.models.WeebApi;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

import static me.melijn.jda.Melijn.OWNERID;
import static me.melijn.jda.Melijn.PREFIX;

public class InfoCommand extends Command {

    public InfoCommand() {
        this.commandName = "info";
        this.usage = PREFIX + this.commandName;
        this.description = "Shows you useful info about the bot itself";
        this.aliases = new String[]{"about", "botinfo", "author"};
        this.category = Category.UTILS;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            try {
                String os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class).getName() +
                        " " + ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class).getArch() +
                        " " + ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class).getVersion();
                int i = 0;
                for (Guild guild : event.getJDA().getGuilds()) {
                    if (guild.getAudioManager().isConnected() || guild.getAudioManager().isAttemptingToConnect()) i++;
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Helpers.EmbedColor);
                eb.setThumbnail(event.getJDA().getSelfUser().getAvatarUrl());
                eb.addField("About", "Melijn is a powerful discord bot coded in java and developed by **ToxicMushroom#2610**\nMore commands/features are being added each month and almost all commands will be configurable in the future" +
                        "\n\n**[Support Server](https://discord.gg/cCDnvNg)** • **[Invite](https://melijn.com/invite?perms=true)** • **[Website](https://melijn.com/)**" + "\n\u200B", false);
                eb.addField("Stats and info",
                        "**Guilds** " + event.getJDA().asBot().getShardManager().getGuilds().size()
                                + "\n**Unique users** " + event.getJDA().asBot().getShardManager().getUsers().size()
                                + "\n**Shards** " + event.getJDA().asBot().getShardManager().getShardsRunning()
                                + "\n**Playing Music Count** " + i
                                + "\n**Operating System** " + os
                                + "\n**CPU Usage** " + getProcessCpuLoad() + "%"
                                + "\n**RAM Usage** " + (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() >> 20) + "MB/" +  (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax() >> 20) + "MB"
                                + "\n**Threads** " + Thread.activeCount()
                                + "\n**Uptime** " + Helpers.getOnlineTime()
                                + "\n\u200B"
                        , false);
                eb.addField("Stuff",
                        "**Java Version** " + Runtime.version().toString().replaceFirst("\\+", " build-") +
                                "\n**JDA Version** " + JDAInfo.VERSION +
                                "\n**Lavaplayer Version** " + PlayerLibrary.VERSION +
                                "\n**Weeb.java Version** " + WeebApi.VERSION
                        , false);
                eb.setFooter("Requested by " + event.getFullAuthorName(), event.getAvatarUrl());
                event.reply(eb.build());
                if (event.getAuthor().getIdLong() == OWNERID) {
                    StringBuilder desc = new StringBuilder();
                    desc.append("```Less\n");
                    int blub = 0;
                    for (Guild guild : event.getJDA().getGuilds()) {
                        if (guild.getAudioManager().isConnected() || guild.getAudioManager().isAttemptingToConnect())
                            desc.append("#").append(++blub).append(" - ").append(guild.getName()).append("\n");
                    }
                    desc.append("```");
                    if (desc.toString().length() > 11)
                        event.getAuthor().openPrivateChannel().queue((channel) -> channel.sendMessage(desc.toString()).queue());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private static double getProcessCpuLoad() throws Exception {
        MBeanServer mbs    = ManagementFactory.getPlatformMBeanServer();
        ObjectName name    = ObjectName.getInstance("java.lang:type=OperatingSystem");
        AttributeList list = mbs.getAttributes(name, new String[]{ "ProcessCpuLoad" });

        if (list.isEmpty())     return Double.NaN;

        Attribute att = (Attribute)list.get(0);
        Double value  = (Double)att.getValue();

        // usually takes a couple of seconds before we get real values
        if (value == -1.0)      return Double.NaN;
        // returns a percentage value with 1 decimal point precision
        return ((int)(value * 1000) / 10.0);
    }
}
