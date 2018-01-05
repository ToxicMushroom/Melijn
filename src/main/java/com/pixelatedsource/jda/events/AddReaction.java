package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;

public class AddReaction extends ListenerAdapter {

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (MusicManager.usersFormToReply.get(event.getUser()) != null) {
            if (event.getMessageId().equals(MusicManager.usersFormToReply.get(event.getUser()).getId())) {
                MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
                if (event.getReactionEmote().getName().equalsIgnoreCase("✅")) {
                    List<AudioTrack> tracks = MusicManager.usersRequest.get(event.getUser());
                    StringBuilder songs = new StringBuilder();
                    int i = player.getListener().getTrackSize();
                    int t = 0;
                    for (AudioTrack track : tracks) {
                        i++;
                        player.playTrack(track);
                        songs.append("[#").append(i).append("](").append(track.getInfo().uri).append(") - ").append(track.getInfo().title).append("\n");
                        if (songs.length() > 1700) {
                            t++;
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setTitle("Added part **#" + t + "**");
                            eb.setColor(Helpers.EmbedColor);
                            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                            eb.setDescription(songs);
                            event.getTextChannel().sendMessage(eb.build()).queue();
                            songs = new StringBuilder();
                        }
                    }
                    if (t == 0) {
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("Added");
                        eb.setColor(Helpers.EmbedColor);
                        eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                        eb.setDescription(songs);
                        event.getTextChannel().sendMessage(eb.build()).queue();
                    } else {
                        t++;
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("Added part **#" + t + "**");
                        eb.setColor(Helpers.EmbedColor);
                        eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                        eb.setDescription(songs);
                        event.getTextChannel().sendMessage(eb.build()).queue();
                    }
                    MusicManager.usersFormToReply.get(event.getUser()).delete().queue();
                    MusicManager.usersFormToReply.remove(event.getUser());
                    MusicManager.usersRequest.remove(event.getUser());
                } else if (event.getReactionEmote().getName().equalsIgnoreCase("❎")) {
                    MusicManager.usersFormToReply.get(event.getUser()).delete().queue();
                    MusicManager.usersFormToReply.remove(event.getUser());
                    MusicManager.usersRequest.remove(event.getUser());
                }
            }
        }
    }
}
