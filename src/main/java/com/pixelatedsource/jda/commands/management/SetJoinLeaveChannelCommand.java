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

    public static HashMap<String, String> welcomChannels = PixelSniper.mySQL.getChannelMap(ChannelType.WELCOME);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                String welcomeChannelId = welcomChannels.getOrDefault(guild.getId(), "null");
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    String id;
                    if (event.getMessage().getMentionedChannels().size() == 1 && event.getMessage().getMentionedChannels().get(0).getType() == net.dv8tion.jda.core.entities.ChannelType.TEXT) {
                        id = event.getMessage().getMentionedChannels().get(0).getId();
                    } else if (args[0].matches("\\d+") && guild.getTextChannelById(args[0]) != null) {
                        id = args[0];
                    } else if (args[0].equalsIgnoreCase("null")) {
                        id = null;
                    } else {
                        MessageHelper.sendUsage(this, event);
                        return;
                    }
                    new Thread(() -> PixelSniper.mySQL.setChannel(guild.getId(), id, ChannelType.WELCOME)).start();
                    if (!SetJoinMessageCommand.joinMessages.containsKey(guild.getId())) {
                        SetJoinMessageCommand.joinMessages.put(guild.getId(), "Welcome **%USERNAME%** to our awesome discord server :D");
                        new Thread(() -> PixelSniper.mySQL.setMessage(guild, "Welcome **%USERNAME%** to our awesome discord server :D", MessageType.JOIN)).start();
                        event.reply("I've set the default join message :beginner:");
                    }
                    if (!SetLeaveMessageCommand.leaveMessages.containsKey(guild.getId())) {
                        SetLeaveMessageCommand.leaveMessages.put(guild.getId(), "**%USERNAME%** left us :C");
                        new Thread(() -> PixelSniper.mySQL.setMessage(guild, "**%USERNAME%** left us :C", MessageType.LEAVE)).start();
                        event.reply("I've set the default leave message :beginner:");
                    }
                    String oldChannel = welcomeChannelId == null || welcomeChannelId.equalsIgnoreCase("null") ? "null" : "<#" + welcomeChannelId + ">";
                    String newChannel = args[0].equalsIgnoreCase("null") ? "null" : "<#" + id + ">";
                    event.reply("WelcomeChannel has been changed from " + oldChannel + " to " + newChannel);
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
