package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import com.pixelatedsource.jda.music.MusicManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class SkipXCommand extends Command {

    public SkipXCommand() {
        this.name = "skipx";
        this.aliases = new String[]{"seek"};
        this.guildOnly = true;
        this.help = "Skip a part of the playing song -> Usage: " + PixelatedBot.PREFIX + this.name + " <xx:xx>";
    }

    private MusicManager manager = MusicManager.getManagerinstance();

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name)) {
            String[] args = event.getArgs().replaceFirst(":", " ").split("\\s+");
            AudioTrack player = manager.getPlayer(event.getGuild()).getAudioPlayer().getPlayingTrack();

            int seconds;
            if (args.length < 2) seconds = 0;
            else try {
                if (args[0] == null || args[0].equalsIgnoreCase("")) args[0] = "0";
                Integer.parseInt(args[0]);
                seconds = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                event.reply("Usage: " + PixelatedBot.PREFIX + this.name + " 1:10");
                e.addSuppressed(e.getCause());
                return;
            }
            if (player != null)
                player.setPosition(Integer.parseInt(args[0]) * 60000 + seconds * 1000);
            else
                event.reply("Their are no songs playing at the moment.");
        }
    }
}
