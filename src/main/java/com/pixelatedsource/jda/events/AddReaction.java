package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.ChannelType;
import com.pixelatedsource.jda.commands.music.SPlayCommand;
import com.pixelatedsource.jda.music.MusicManager;
import com.pixelatedsource.jda.music.MusicPlayer;
import com.pixelatedsource.jda.utils.MessageHelper;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;

public class AddReaction extends ListenerAdapter {

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (PixelSniper.mySQL.getChannelId(event.getGuild().getId(), ChannelType.LOG) != null && PixelSniper.mySQL.getChannelId(event.getGuild().getId(), ChannelType.LOG).equalsIgnoreCase(event.getTextChannel().getId())) {
            if (event.getReactionEmote().getName().equalsIgnoreCase("\uD83D\uDD30")) {
                if (Helpers.hasPerm(event.getMember(), "emote.claim", 1)) {
                    String messageid = PixelSniper.mySQL.getMessageIdByUnclaimedId(event.getMessageId());
                    if (messageid != null) {
                        event.getChannel().getMessageById(event.getMessageId()).queue(v -> v.editMessage(PixelSniper.mySQL.unclaimedToClaimed(messageid, event.getJDA(), event.getUser())).queue());
                    }
                }
            }
        }
        if (PixelSniper.mySQL.getChannelId(event.getGuild().getId(), ChannelType.LOG) != null) {
            if (event.getReactionEmote().getName().equalsIgnoreCase("\u274C")) { //:x: emote red cross
                if (Helpers.hasPerm(event.getMember(), "emote.delete", 1)) {
                    String messageid = event.getMessageId();
                    MessageHelper.deletedByEmote.put(messageid, event.getUser());
                    event.getChannel().getMessageById(messageid).queue(v -> v.delete().queue());
                }
            }
        }
        if (MusicManager.usersFormToReply.get(event.getUser()) != null && MusicManager.usersFormToReply.get(event.getUser()).getId().equalsIgnoreCase(event.getMessageId())) {
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
        if (SPlayCommand.usersFormToReply.get(event.getUser()) != null && SPlayCommand.usersFormToReply.get(event.getUser()).getId().equalsIgnoreCase(event.getMessageId())) {
            MusicPlayer player = MusicManager.getManagerinstance().getPlayer(event.getGuild());
            AudioTrack track;
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Added");
            eb.setFooter(Helpers.getFooterStamp(), null);
            eb.setColor(Helpers.EmbedColor);
            switch (event.getReactionEmote().getName()) {
                case "\u0031\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser()).get(0);
                    player.playTrack(track);
                    eb.setDescription("`" + track.getInfo().title + "` added to the queue at postition **#" + player.getListener().getTrackSize() + "**");
                    event.getTextChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u0032\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser()).get(1);
                    player.playTrack(track);
                    eb.setDescription("`" + track.getInfo().title + "` added to the queue at postition **#" + player.getListener().getTrackSize() + "**");
                    event.getTextChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u0033\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser()).get(2);
                    player.playTrack(track);
                    eb.setDescription("`" + track.getInfo().title + "` added to the queue at postition **#" + player.getListener().getTrackSize() + "**");
                    event.getTextChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u0034\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser()).get(3);
                    player.playTrack(track);
                    eb.setDescription("`" + track.getInfo().title + "` added to the queue at postition **#" + player.getListener().getTrackSize() + "**");
                    event.getTextChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u0035\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser()).get(4);
                    player.playTrack(track);
                    eb.setDescription("`" + track.getInfo().title + "` added to the queue at postition **#" + player.getListener().getTrackSize() + "**");
                    event.getTextChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u274E":
                    SPlayCommand.usersFormToReply.remove(event.getUser());
                    SPlayCommand.userChoices.remove(event.getUser());
                    event.getTextChannel().getMessageById(event.getMessageId()).queue(s -> s.delete().queue());
                    break;
            }
        }
    }
}
