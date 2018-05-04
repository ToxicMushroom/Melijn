package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.ChannelType;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetLogChannelCommand extends Command {

    public SetLogChannelCommand() {
        this.commandName = "setlogchannel";
        this.description = "Set change or view the text channel where the bot has to send the messages";
        this.usage = PREFIX + commandName + " [channelId | #channel | null]";
        this.aliases = new String[]{"slc"};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                String logChannelId = PixelSniper.mySQL.getChannelId(event.getGuild().getId(), ChannelType.LOG);
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    String id;
                    if (event.getMessage().getMentionedChannels().size() == 1) {
                        id = event.getMessage().getMentionedChannels().get(0).getId();
                    } else if (args[0].matches("\\d+") && event.getGuild().getTextChannelById(args[0]) != null) {
                        id = event.getGuild().getTextChannelById(args[0]).getId();
                    } else if (args[0].equalsIgnoreCase("null")) {
                        id = null;
                    } else {
                        MessageHelper.sendUsage(this, event);
                        return;
                    }
                    if (PixelSniper.mySQL.setChannel(event.getGuild().getId(), id, ChannelType.LOG)) {
                        event.reply("LogChannel has been changed from <#" + logChannelId + "> to <#" + PixelSniper.mySQL.getChannelId(event.getGuild().getId(), ChannelType.LOG) + ">");
                    }
                } else {
                    event.reply("<#" + logChannelId + ">");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
