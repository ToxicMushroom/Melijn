package com.pixelatedsource.jda.music;

import com.pixelatedsource.jda.Helpers;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicManager {

    private final AudioPlayerManager manager = new DefaultAudioPlayerManager();
    private final Map<String, MusicPlayer> players = new HashMap<>();
    private static MusicManager managerinstance = new MusicManager();

    private MusicManager() {
        AudioSourceManagers.registerRemoteSources(manager);
        AudioSourceManagers.registerLocalSource(manager);
    }

    public static MusicManager getManagerinstance() {
        return managerinstance;
    }

    public synchronized MusicPlayer getPlayer(Guild guild) {
        if (!players.containsKey(guild.getId()))
            players.put(guild.getId(), new MusicPlayer(manager.createPlayer(), guild));
        return players.get(guild.getId());
    }

    public void loadTrack(final TextChannel channel, final String source) {
        MusicPlayer player = getPlayer(channel.getGuild());
        channel.getGuild().getAudioManager().setSendingHandler(player.getAudioHandler());
        manager.loadItemOrdered(player, source, new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Added");
                eb.setDescription("`" + track.getInfo().title + "` added to the queue at postition **#" + player.getListener().getTrackSize()+ "**");
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                eb.setColor(Helpers.EmbedColor);
                channel.sendMessage(eb.build()).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                if (tracks.size() > 1)
                    tracks = tracks.subList(0, 1); // First 5 tracks from playlist (0 index)

                for (AudioTrack track : tracks) {
                    player.playTrack(track);
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Added");
                    eb.setDescription("`" + track.getInfo().title + "` added to the queue at postition #" + player.getListener().getTrackSize());
                    eb.setDescription("`" + track.getInfo().title + "` added to the queue at postition **#" + player.getListener().getTrackSize()+ "**");
                    eb.setColor(Helpers.EmbedColor);
                    channel.sendMessage(eb.build()).queue();
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
}
