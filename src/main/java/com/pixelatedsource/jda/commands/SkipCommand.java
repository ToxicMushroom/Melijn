package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

public class SkipCommand extends Command {

    MusicManager manager = MusicManager.getManagerinstance();

    public SkipCommand() {
        this.name = "skip";
        this.help = "skip a song";
    }

    @Override
    protected void execute(CommandEvent event) {
        Guild guild = event.getGuild();
        MusicPlayer player = manager.getPlayer(guild);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        EmbedBuilder eb = new EmbedBuilder();
        BlockingQueue<AudioTrack> audioTracks = player.getListener().getTracks();
        int i = 0;
        AudioTrack nextSong = null;
        for(AudioTrack track : audioTracks) {
            if (i == 1) {
            } else {
                nextSong = track;
                i++;
            }
        }
        eb.setTitle("Skipped");
        eb.setColor(Helpers.EmbedColor);
        if (nextSong != null) {
            eb.setDescription("Skipped: " + player.getAudioPlayer().getPlayingTrack().getInfo().title + "\n" + "Now playing: " + nextSong.getInfo().title);
        } else {  eb.setDescription("Skipped: " + player.getAudioPlayer().getPlayingTrack().getInfo().title + "\n" + "No next song."); }
        eb.setFooter("ToxicMushroom | " + simpleDateFormat.format(date), "https://i.imgur.com/1wj6Jlr.png");
        event.reply(eb.build());
        player.skipTrack();
    }
}
