package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.*;
import com.pixelatedsource.jda.db.MySQL;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetJoinLeaveChannelCommand extends Command {

    public SetJoinLeaveChannelCommand() {
        this.commandName = "setjoinleavechannel";
        this.description = "Setup a textchannel where users will be welcomed or leave";
        this.usage = PREFIX + commandName + " <TextChannel | null>";
        this.aliases = new String[]{"sjlc", "setwelcomechannel", "swc"};
        this.category = Category.MANAGEMENT;
    }

    MySQL mySQL = PixelSniper.mySQL;

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                String welcomeChannelId = mySQL.getChannelId(guild.getId(), ChannelType.WELCOME);
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
                    if (mySQL.setChannel(guild.getId(), id, ChannelType.WELCOME)) {
                        if (mySQL.getMessage(guild, MessageType.JOIN) == null) mySQL.setMessage(guild, "Welcome **%USERNAME%** to our awesome discord server :D", MessageType.JOIN);
                        if (mySQL.getMessage(guild, MessageType.LEAVE) == null) mySQL.setMessage(guild,"**%USERNAME%** left us :C", MessageType.JOIN);
                        String oldChannel = welcomeChannelId == null || welcomeChannelId.equalsIgnoreCase("null") ? "null" : "<#" + welcomeChannelId + ">";
                        String newChannel = args[0].equalsIgnoreCase("null") ? "null" : "<#" + id + ">";
                        event.reply("WelcomeChannel has been changed from " + oldChannel + " to " + newChannel);
                    } else {
                        event.reply("Failed to set WelcomeChannel");
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
