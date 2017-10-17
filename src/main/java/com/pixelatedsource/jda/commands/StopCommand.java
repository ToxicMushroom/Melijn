package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import net.dv8tion.jda.core.EmbedBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StopCommand extends Command {

    public StopCommand() {
        this.name = "stop";
        this.help = "stops the current song";
    }

    @Override
    protected void execute(CommandEvent event) {
        PixelatedBot.looped.put(event.getGuild(), false);
        MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        player.stopTrack();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Helpers.EmbedColor);
        eb.setTitle("Stopped");
        eb.setDescription("**I stopped all the songs and left the channel. To resume listening to the songs use !play.**");
        eb.setFooter("ToxicMushroom | " + simpleDateFormat.format(date), "https://i.imgur.com/1wj6Jlr.png");
        event.getTextChannel().sendMessage(eb.build()).queue();
    }
}
