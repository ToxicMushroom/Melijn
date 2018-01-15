package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.ChannelType;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetMusicChannelCommand extends Command {

    public SetMusicChannelCommand() {
        this.commandName = "setmusicchannel";
        this.description = "Set the music channel to a channel so the bot wil auto join ect";
        this.usage = PREFIX + commandName + " [id]";
        this.aliases = new String[]{"smc"};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                String channel = PixelSniper.mySQL.getChannelId(event.getGuild().getId(), ChannelType.MUSIC);
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    event.reply(channel);
                } else if (args[0].matches("\\d+")) {
                    if (PixelSniper.mySQL.setChannel(event.getGuild().getId(), args[0], ChannelType.MUSIC)) {
                        event.reply("Music channel changed from " + channel + " to " + PixelSniper.mySQL.getChannelId(event.getGuild().getId(), ChannelType.MUSIC));
                    } else {
                        event.reply("Failed to set music channel.");
                    }
                }
            }
        }
    }
}
