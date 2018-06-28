package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.pixelatedsource.jda.utils.MessageHelper;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SpeedCommand extends Command {

    public SpeedCommand() {
        this.commandName = "speed";
        this.description = "Change the playback speed of bot";
        this.usage = PREFIX + this.commandName + " [value]";
        this.category = Category.MUSIC;
        this.extra = "1.0 is normal speed";
        this.needs = new Need[] {Need.GUILD};

    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
            if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                if (event.getGuild().getSelfMember().getVoiceState().getChannel() != null) {
                    if (event.getMember().getVoiceState().getChannel() == event.getGuild().getSelfMember().getVoiceState().getChannel()) {
                        if (args[0].matches("[0-9]|[0-9][0-9]|100|[0-9]?[0-9].[0-9]?[0-9]?[0-9]")) {
                            if (player != null && player.getAudioPlayer().getPlayingTrack() != null) {
                                if (Double.parseDouble(args[0]) > 0) {
                                    player.getAudioPlayer().setPaused(false);
                                    player.setSpeed(Double.parseDouble(args[0]));
                                    player.updateFilters();
                                } else {
                                    player.setSpeed(0);
                                    player.getAudioPlayer().setPaused(true);
                                    player.updateFilters();
                                }
                                event.reply("The music speed has been set to **" + args[0] + "** by **" + event.getFullAuthorName() + "**");
                            } else {
                                event.reply("There is no music playing");
                            }
                        } else {
                            MessageHelper.sendUsage(this, event);
                        }
                    } else {
                        event.reply("You have to be in the same voice channel as me to change the speed");
                    }
                } else {
                    event.reply("I'm not in a voiceChannel");
                }
            } else {
                event.reply("The configured speed is **" + player.getSpeed() + "**");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
