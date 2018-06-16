package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.ChannelType;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetLogChannelCommand extends Command {

    public SetLogChannelCommand() {
        this.commandName = "setlogchannel";
        this.description = "Set change or view the text channel where the bot has to send the messages";
        this.usage = PREFIX + commandName + " [channelId | #channel | null]";
        this.aliases = new String[]{"slc"};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, Long> guildLogChannelMap = PixelSniper.mySQL.getChannelMap(ChannelType.LOG);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                String logChannelName = guildLogChannelMap.containsKey(guild.getIdLong()) ? "<#" + guildLogChannelMap.get(guild.getIdLong()) + ">" : "LogChannel is unset";
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    Long id;
                    if (event.getMessage().getMentionedChannels().size() == 1) {
                        id = event.getMessage().getMentionedChannels().get(0).getIdLong();
                    } else if (args[0].matches("\\d+") && guild.getTextChannelById(args[0]) != null) {
                        id = Long.valueOf(args[0]);
                    } else if (args[0].equalsIgnoreCase("null")) {
                        id = 0L;
                    } else {
                        MessageHelper.sendUsage(this, event);
                        return;
                    }
                    if (id == 0L) {
                        guildLogChannelMap.remove(guild.getIdLong());
                        new Thread(() -> PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.LOG)).start();
                        event.reply("LogChannel has been changed from " + logChannelName + " to nothing by **" + event.getFullAuthorName() + "**");
                    } else {
                        guildLogChannelMap.put(guild.getIdLong(), id);
                        new Thread(() -> PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.LOG)).start();
                        event.reply("LogChannel has been changed from " + logChannelName + " to <#" + id + "> by **" + event.getFullAuthorName() + "**");
                    }
                } else {
                    event.reply(logChannelName);
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
