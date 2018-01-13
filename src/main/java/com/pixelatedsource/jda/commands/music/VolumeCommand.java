package com.pixelatedsource.jda.commands.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class VolumeCommand extends Command {

    public VolumeCommand() {
        this.commandName = "volume";
        this.usage = PREFIX + this.commandName + " <0-100>";
        this.description = "Change or view the volume of the music for everyone";
        this.aliases = new String[] {"vol"};
        this.category = Category.MUSIC;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                String args[] = event.getArgs().split("\\s+");
                MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
                int volume;
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    event.reply("Current volume: **" + Math.round(player.getAudioPlayer().getVolume() / 1.5) + "%**");
                    return;
                }
                try {
                    volume = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    return;
                }
                if (volume >= 0 && volume <= 100) {
                    player.getAudioPlayer().setVolume(Integer.parseInt(String.valueOf(Math.round(volume * 1.5))));
                    event.reply("Volume has been set to **" + String.valueOf(Math.round((double) volume)) + "%**");
                } else {
                    event.reply("no no no, use 0-100. default: 40");
                }
            }
        }
    }
}

