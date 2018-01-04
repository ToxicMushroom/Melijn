package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class QueueCommand extends Command {

    public QueueCommand() {
        this.name = "queue";
        this.help = "Usage: " + PixelatedBot.PREFIX + this.name;
        this.guildOnly = true;
    }

    private MusicManager manager = MusicManager.getManagerinstance();

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name)) {
            Guild guild = event.getGuild();
            MusicPlayer player = manager.getPlayer(guild);
            if (player.getListener().getTrackSize() == 0 && player.getAudioPlayer().getPlayingTrack() == null) {
                event.reply("Nothing here..");
                return;
            }
            BlockingQueue<AudioTrack> tracks = player.getListener().getTracks();;
            List<String> lijst = new ArrayList<>();
            int i = 0;
            if (player.getAudioPlayer().getPlayingTrack() != null) {
                lijst.add(String.valueOf("[#" + i + "](" + player.getAudioPlayer().getPlayingTrack().getInfo().uri + ") - `Now playing:` " + player.getAudioPlayer().getPlayingTrack().getInfo().title + " `" + Helpers.getDurationBreakdown(player.getAudioPlayer().getPlayingTrack().getInfo().length) + "`"));
            }
            for (AudioTrack track : tracks) {
                i++;
                lijst.add(String.valueOf("[#" + i + "](" + track.getInfo().uri + ") - " + track.getInfo().title + " `" + Helpers.getDurationBreakdown(track.getInfo().length) + "`"));
            }
            StringBuilder builder = new StringBuilder();
            for (String s : lijst) {
                builder.append(s).append("\n");
            }
            if (builder.toString().length() > 1800) {
                int part = 1;
                builder = new StringBuilder();
                for (String s : lijst) {
                    if (builder.toString().length() + s.length() > 1800) {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("Queue part " + part);
                        eb.setColor(Helpers.EmbedColor);
                        eb.setDescription(builder.toString());
                        eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                        event.reply(eb.build());
                        builder = new StringBuilder();
                        part++;
                    }
                    builder.append(s).append("\n");
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Queue part " + part);
                eb.setColor(Helpers.EmbedColor);
                eb.setDescription(builder.toString());
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                event.reply(eb.build());
            } else {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Queue");
                eb.setColor(Helpers.EmbedColor);
                eb.setDescription(builder.toString());
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                event.reply(eb.build());
            }
        }
    }
}
