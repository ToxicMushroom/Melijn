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

public class SetMusicChannelCommand extends Command {

    public SetMusicChannelCommand() {
        this.commandName = "setmusicchannel";
        this.description = "Set the music channel to a channel so the bot wil auto join ect";
        this.usage = PREFIX + commandName + " [id]";
        this.aliases = new String[]{"smc"};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<String, String> musicChannelIds = PixelSniper.mySQL.getChannelMap(ChannelType.MUSIC);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                String channelId = musicChannelIds.get(guild.getId());
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    event.reply("<#" + channelId + ">");
                } else if (args[0].matches("\\d+") && guild.getVoiceChannelById(args[0]) != null) {
                    if (musicChannelIds.containsKey(guild.getId())) musicChannelIds.replace(guild.getId(), args[0]);
                    else musicChannelIds.put(guild.getId(), args[0]);
                    new Thread(() -> PixelSniper.mySQL.setChannel(guild.getId(), args[0], ChannelType.MUSIC)).start();
                    event.reply("Music channel changed from <#" + channelId + "> to <#" + args[0] + ">");
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
