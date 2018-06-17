package com.pixelatedsource.jda.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.commands.music.SPlayCommand;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MusicManager {

    private final AudioPlayerManager manager = new DefaultAudioPlayerManager();
    private final Map<Long, MusicPlayer> players = new HashMap<>();
    private static MusicManager managerinstance = new MusicManager();
    public static HashMap<User, List<AudioTrack>> usersRequest = new HashMap<>();
    public static HashMap<User, Message> usersFormToReply = new HashMap<>();

    public MusicManager() {
        AudioSourceManagers.registerRemoteSources(manager);
        AudioSourceManagers.registerLocalSource(manager);
    }

    public static MusicManager getManagerinstance() {
        return managerinstance;
    }

    public synchronized MusicPlayer getPlayer(Guild guild) {
        if (!players.containsKey(guild.getIdLong()))
            players.put(guild.getIdLong(), new MusicPlayer(manager.createPlayer(), guild));
        return players.get(guild.getIdLong());
    }

    public void loadTrack(final TextChannel channel, final String source, User requester, boolean isPlaylist) {
        MusicPlayer player = getPlayer(channel.getGuild());
        channel.getGuild().getAudioManager().setSendingHandler(player.getAudioHandler());
        manager.loadItemOrdered(player, source, new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Added");
                eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at postition **#" + player.getListener().getTrackSize() + "**");
                eb.setFooter(Helpers.getFooterStamp(), null);
                eb.setColor(Helpers.EmbedColor);
                channel.sendMessage(eb.build()).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                if (!isPlaylist) {
                    trackLoaded(tracks.get(0));
                } else {
                    if (tracks.size() > 50)
                        tracks = tracks.subList(0, 50);
                    if (usersRequest.get(requester) == null && usersFormToReply.get(requester) == null) {
                        usersRequest.put(requester, tracks);
                        StringBuilder songs = new StringBuilder();
                        for (AudioTrack track : tracks) {
                            songs.append(track.getInfo().title).append("\n");
                        }
                        String toSend = String.valueOf("You're about to add a playlist which contains these songs:\n" + songs + "Hit :white_check_mark: to accept or :negative_squared_cross_mark: to deny").length() < 1999 ?
                                "You're about to add a playlist which contains these songs:\n" + songs + "Hit :white_check_mark: to accept or :negative_squared_cross_mark: to deny" : //true before :
                                "You're about to add a playlist which contains " + tracks.size() + " songs.\nHit :white_check_mark: to accept or :negative_squared_cross_mark: to deny.";
                        channel.sendMessage(toSend).queue(v -> {
                            usersFormToReply.put(requester, v);
                            v.addReaction("\u2705").queue();
                            v.addReaction("\u274E").queue();
                            Helpers.waitForIt(requester);
                            v.delete().queueAfter(30, TimeUnit.SECONDS);
                        });
                    } else {
                        channel.sendMessage("You still have a request to answer. (requests automatically get removed after 30 seconds)")
                                .queue(v -> v.delete().queueAfter(10, TimeUnit.SECONDS));
                    }
                }
            }

            @Override
            public void noMatches() {
                channel.sendMessage("I couldn't find a song called " + source + ". Check on spelling mistakes.").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Error: " + exception.getMessage()).queue();
            }
        });
    }

    public void loadSimpelTrack(Guild guild, final String source) {
        MusicPlayer player = getPlayer(guild);
        guild.getAudioManager().setSendingHandler(player.getAudioHandler());
        manager.loadItemOrdered(player, source, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                if (tracks.size() > 5)
                    tracks = tracks.subList(0, 1); // First 5 tracks from playlist (0 index)

                for (AudioTrack track : tracks) {
                    player.playTrack(track);
                }
            }

            @Override
            public void noMatches() {
            }

            @Override
            public void loadFailed(FriendlyException exception) {
            }
        });
    }

    public void searchTracks(TextChannel channel, String source, User author) {
        MusicPlayer player = getPlayer(channel.getGuild());
        channel.getGuild().getAudioManager().setSendingHandler(player.getAudioHandler());
        manager.loadItemOrdered(player, source, new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                loadTrack(channel, source, author, false);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Select Menu");
                eb.setColor(Helpers.EmbedColor);
                eb.setFooter(Helpers.getFooterStamp(), null);
                StringBuilder sb = new StringBuilder();
                HashMap<Integer, AudioTrack> map = new HashMap<>();
                int i = 0;
                for (AudioTrack track : tracks) {
                    map.put(i, track);
                    if (i != 5)
                    sb.append("[").append(++i).append("](").append(track.getInfo().uri).append(") - ").append(track.getInfo().title).append(" `[").append(Helpers.getDurationBreakdown(track.getInfo().length)).append("]`\n");
                }
                eb.setDescription(sb.toString());
                SPlayCommand.userChoices.put(author, map);
                channel.sendMessage(eb.build()).queue((s) -> {
                    SPlayCommand.usersFormToReply.put(author, s);
                    s.addReaction("\u0031\u20E3").queue();
                    s.addReaction("\u0032\u20E3").queue();
                    s.addReaction("\u0033\u20E3").queue();
                    s.addReaction("\u0034\u20E3").queue();
                    s.addReaction("\u0035\u20E3").queue();
                    s.addReaction("\u274E").queue();
                });
            }

            @Override
            public void noMatches() {
                channel.sendMessage("I couldn't find a song called " + source + ". Check on spelling mistakes.").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Error: " + exception.getMessage()).queue();
            }
        });
    }
}
