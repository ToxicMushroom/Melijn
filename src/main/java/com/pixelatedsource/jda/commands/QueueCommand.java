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

public class QueueCommand extends Command{
    MusicManager manager = MusicManager.getManagerinstance();

    public QueueCommand() {
        this.name = "queue";
        this.help = "shows all songs in the queue";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        Guild guild = event.getGuild();
        MusicPlayer player = manager.getPlayer(guild);
        System.out.println(player.getListener().getTrackSize());
        if (player.getListener().getTrackSize() == 0 && player.getAudioPlayer().getPlayingTrack() == null) return;
        BlockingQueue<AudioTrack> tracks = player.getListener().getTracks();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Helpers.EmbedColor);
        eb.setTitle("Queue");
        StringBuilder sb = new StringBuilder();
        int i = 0;
        if (player.getAudioPlayer().getPlayingTrack() != null) {
            sb.append(String.valueOf("[" + i + "](" + player.getAudioPlayer().getPlayingTrack().getInfo().uri + ") " + player.getAudioPlayer().getPlayingTrack().getInfo().title + " `" + Helpers.getDurationBreakdown(player.getAudioPlayer().getPlayingTrack().getInfo().length)+"`"));
        }
        for (AudioTrack track : tracks) {
            i++;
            sb.append(String.valueOf("\n[" + i + "](" + track.getInfo().uri + ") " + track.getInfo().title + " `" + Helpers.getDurationBreakdown(track.getInfo().length) + "`"));
        }
        eb.setDescription(sb);
        eb.setFooter("ToxicMushroom | " + simpleDateFormat.format(date), "https://i.imgur.com/1wj6Jlr.png");
        event.getTextChannel().sendMessage(eb.build()).queue();
    }
}
