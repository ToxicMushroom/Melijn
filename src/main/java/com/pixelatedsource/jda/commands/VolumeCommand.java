package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import com.pixelatedsource.jda.music.MusicManager;

public class VolumeCommand extends Command {

    public VolumeCommand() {
        this.name = "volume";
        this.help = "Set the volume of the player -> Usage: " + PixelatedBot.PREFIX + this.name + " <0-100>";
        this.guildOnly = true;
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name)) {
            String args[] = event.getArgs().split("\\s+");
            int volume;
            if (args.length == 0) {
                event.reply("Provide a number between 0 and 100");
                return;
            }
            try {
                volume = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                return;
            }
            if (volume >= 0 && volume <= 100) {
                MusicManager.getManagerinstance().getPlayer(event.getGuild()).getAudioPlayer().setVolume(Integer.parseInt(String.valueOf(Math.round(volume * 1.5))));
                event.reply("Volume has been set to " + String.valueOf(Math.round((double) volume)) + "%");
            } else {
                event.reply("no no no, use 0-100. default: 40");
            }
        }
    }
}

