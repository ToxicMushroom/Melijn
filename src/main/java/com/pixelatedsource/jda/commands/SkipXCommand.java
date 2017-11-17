package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.PixelatedBot;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;

public class SkipXCommand extends Command {
    public SkipXCommand() {
        this.name = "skipx";
        this.help = "skip to a sercaint part inside of a song";
    }

    MusicManager manager = MusicManager.getManagerinstance();

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().replaceFirst(":"," ").split("\\s+");
        MusicPlayer player = manager.getPlayer(event.getGuild());
        int minutes;
        int seconds;
        long time;
        try {
           minutes = Integer.parseInt(args[0]);
           seconds = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            e.addSuppressed(e);
            event.reply("Usage: " + PixelatedBot.PREFIX + this.name + " 1:");
            return;
        }
        time = minutes * 60000 + seconds * 1000;
        player.getAudioPlayer().getPlayingTrack().setPosition(time);
    }
}
