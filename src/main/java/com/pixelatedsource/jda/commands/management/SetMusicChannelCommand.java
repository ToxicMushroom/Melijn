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
        this.commandName = "setMusicChannel";
        this.description = "Set the music channel to a channel so the bot wil auto join ect";
        this.usage = PREFIX + commandName + " [id/null]";
        this.aliases = new String[]{"smc"};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, Long> musicChannelIds = PixelSniper.mySQL.getChannelMap(ChannelType.MUSIC);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    event.reply(musicChannelIds.containsKey(guild.getIdLong()) ? "MusicChannel: <#" + musicChannelIds.get(guild.getIdLong()) + ">" : "The MusicChannel is unset");
                } else {
                    if (args[0].matches("\\d+") && guild.getVoiceChannelById(args[0]) != null) {
                        if (musicChannelIds.replace(guild.getIdLong(), Long.valueOf(args[0])) == null)
                            musicChannelIds.put(guild.getIdLong(), Long.valueOf(args[0]));
                        new Thread(() -> PixelSniper.mySQL.setChannel(guild.getIdLong(), Long.parseLong(args[0]), ChannelType.MUSIC)).start();
                        event.reply("MusicChannel has been set to <#" + args[0] + "> by **" + event.getFullAuthorName() + "**");
                    } else if (args[0].equalsIgnoreCase("null")) {
                       musicChannelIds.remove(guild.getIdLong());
                       new Thread(() -> PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.MUSIC)).start();
                        event.reply("MusicChannel has been unset **" + event.getFullAuthorName() + "**");
                    } else {
                        MessageHelper.sendUsage(this, event);
                    }
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
