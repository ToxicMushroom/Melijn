package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.*;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetJoinLeaveChannelCommand extends Command {

    public SetJoinLeaveChannelCommand() {
        this.commandName = "setjoinleavechannel";
        this.description = "Setup a textchannel where users will be welcomed or leave";
        this.usage = PREFIX + commandName + " <TextChannel | null>";
        this.aliases = new String[]{"sjlc", "setwelcomechannel", "swc"};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, Long> welcomeChannels = PixelSniper.mySQL.getChannelMap(ChannelType.WELCOME);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                long welcomeChannelId = welcomeChannels.getOrDefault(guild.getIdLong(), -1L);
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    long id;
                    if (event.getMessage().getMentionedChannels().size() == 1 && event.getMessage().getMentionedChannels().get(0).getType() == net.dv8tion.jda.core.entities.ChannelType.TEXT) {
                        id = event.getMessage().getMentionedChannels().get(0).getIdLong();
                    } else if (args[0].matches("\\d+") && guild.getTextChannelById(args[0]) != null) {
                        id = Long.parseLong(args[0]);
                    } else if (args[0].equalsIgnoreCase("null")) {
                        id = -1L;
                    } else {
                        MessageHelper.sendUsage(this, event);
                        return;
                    }
                    if (id != -1L) {
                        new Thread(() -> PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.WELCOME)).start();
                        if (!SetJoinMessageCommand.joinMessages.containsKey(guild.getIdLong())) {
                            SetJoinMessageCommand.joinMessages.put(guild.getIdLong(), "Welcome %USER% to our awesome discord server :D");
                            new Thread(() -> PixelSniper.mySQL.setMessage(guild.getIdLong(), "Welcome %USER% to our awesome discord server :D", MessageType.JOIN)).start();
                            event.reply("I've set the default join message :beginner:");
                        }
                        if (!SetLeaveMessageCommand.leaveMessages.containsKey(guild.getIdLong())) {
                            SetLeaveMessageCommand.leaveMessages.put(guild.getIdLong(), "**%USERNAME%** left us :C");
                            new Thread(() -> PixelSniper.mySQL.setMessage(guild.getIdLong(), "**%USERNAME%** left us :C", MessageType.LEAVE)).start();
                            event.reply("I've set the default leave message :beginner:");
                        }
                        if (welcomeChannels.replace(guild.getIdLong(), welcomeChannelId) == null) {
                            welcomeChannels.put(guild.getIdLong(), welcomeChannelId);
                        }
                        String oldChannel = welcomeChannelId == -1 ? "nothing" : "<#" + welcomeChannelId + ">";
                        String newChannel = "<#" + id + ">";
                        event.reply("WelcomeChannel has been changed from " + oldChannel + " to " + newChannel);
                    } else {
                        long oldChannel = welcomeChannels.getOrDefault(guild.getIdLong(), -1L);
                        welcomeChannels.remove(guild.getIdLong());
                        event.reply("WelcomeChannel has been changed from " + (oldChannel == -1L ? "nothing" : "<#" + oldChannel + ">") + " to nothing");
                    }
                } else {
                    event.reply("Current WelcomeChannel: <#" + welcomeChannelId + ">");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
