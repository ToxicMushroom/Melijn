package com.pixelatedsource.jda.commands;

import com.pixelatedsource.jda.PixelatedBot;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.awt.*;

public class MonstercatCommand extends ListenerAdapter {

    AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        String[] cmd = e.getMessage().getContent().split(" ");
        String label = cmd[0];
        //String message = e.getMessage().getContent();

        if (!label.startsWith(PixelatedBot.PREFIX)) return;
        if (label.equalsIgnoreCase(PixelatedBot.PREFIX + "Monstercat")) {
            String msg = "I am now in the music channel playing the monstercat stream from twitch.";
            e.getChannel().sendMessage(msg).queue();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.CYAN);
            eb.setDescription(msg);
            eb.setThumbnail("https://iminco.nl/wp-content/uploads/kip-1024x512.jpg");
            eb.setTitle("Joined");
            e.getChannel().sendMessage(eb.build()).queue();
        }
    }

}
