package com.pixelatedsource.jda.commands.music;

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

public class setMusicLogChannel extends Command {

    public setMusicLogChannel() {
        this.commandName = "setMusicLogChannel";
        this.usage = PREFIX + commandName + " [textChannel]";
        this.description = "Set a musicLogChannel where the bot will send the nowPlaying songs";
        this.aliases = new String[]{"smlc"};
        this.category = Category.MUSIC;
    }

    HashMap<Long, Long> musicLogChannelMap = PixelSniper.mySQL.getChannelMap(ChannelType.NOWPLAYING);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                String logChannelName = musicLogChannelMap.containsKey(guild.getIdLong()) ? "<#" + musicLogChannelMap.get(guild.getIdLong()) + ">" : "LogChannel is unset";
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    Long id = Helpers.getChannelByArgsN(event, args[0]);
                    if (id != -1L) {
                        if (id == 0L) {
                            musicLogChannelMap.remove(guild.getIdLong());
                            new Thread(() -> PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.NOWPLAYING)).start();
                            event.reply("MusicLogChannel has been changed from " + logChannelName + " to nothing");
                        } else {
                            musicLogChannelMap.put(guild.getIdLong(), id);
                            new Thread(() -> PixelSniper.mySQL.setChannel(guild.getIdLong(), id, ChannelType.NOWPLAYING)).start();
                            event.reply("MusicLogChannel has been changed from " + logChannelName + " to <#" + id + ">");
                        }
                    } else {
                        MessageHelper.sendUsage(this, event);
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
