package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.pixelatedsource.jda.utils.MessageHelper;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;

public class AddReaction extends ListenerAdapter {

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (PixelSniper.mySQL.getLogChannelId(event.getGuild().getId()) != null && PixelSniper.mySQL.getLogChannelId(event.getGuild().getId()).equalsIgnoreCase(event.getTextChannel().getId())) {
            if (event.getReactionEmote().getName().equalsIgnoreCase("\uD83D\uDD30")) {
                if (Helpers.hasPerm(event.getMember(), "emote.claim", 1)) {
                    String messageid = PixelSniper.mySQL.getMessageIdByUnclaimedId(event.getMessageId());
                    if (messageid != null) {
                        event.getChannel().getMessageById(event.getMessageId()).queue(v -> v.editMessage(PixelSniper.mySQL.unclaimedToClaimed(messageid, event.getJDA(), event.getUser())).queue());
                    }
                }
            } else if (event.getReactionEmote().getName().equalsIgnoreCase("\u274C")){ //:x: emote red cross
                if (event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_READ)) {
                    String messageid = event.getMessageId();
                    MessageHelper.deletedByEmote.put(messageid, event.getUser());
                    event.getChannel().getMessageById(messageid).queue(v -> v.delete().queue());
                }
            }
        }
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
