package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.*;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetMusicChannelCommand extends Command {

    public SetMusicChannelCommand() {
        this.commandName = "setMusicChannel";
        this.description = "Set the music channel to a channel so the bot wil auto join ect";
        this.usage = PREFIX + commandName + " [VoiceChannel | null]";
        this.aliases = new String[]{"smc"};
        this.needs = new Need[]{Need.GUILD};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, Long> musicChannelIds = PixelSniper.mySQL.getChannelMap(ChannelType.MUSIC);

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                event.reply(musicChannelIds.containsKey(guild.getIdLong()) ? "MusicChannel: <#" + musicChannelIds.get(guild.getIdLong()) + ">" : "The MusicChannel is unset");
            } else {
                long channelId = Helpers.getVoiceChannelByArgsN(event, args[0]);
                if (channelId == -1) {
                    MessageHelper.sendUsage(this, event);
                } else if (channelId == 0) {
                    musicChannelIds.remove(guild.getIdLong());
                    new Thread(() -> PixelSniper.mySQL.removeChannel(guild.getIdLong(), ChannelType.MUSIC)).start();
                    event.reply("The MusicChannel has been unset by **" + event.getFullAuthorName() + "**");
                } else {
                    if (musicChannelIds.replace(guild.getIdLong(), channelId) == null)
                        musicChannelIds.put(guild.getIdLong(), channelId);
                    new Thread(() -> PixelSniper.mySQL.setChannel(guild.getIdLong(), channelId, ChannelType.MUSIC)).start();
                    event.reply("The MusicChannel has been set to <#" + args[0] + "> by **" + event.getFullAuthorName() + "**");
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
