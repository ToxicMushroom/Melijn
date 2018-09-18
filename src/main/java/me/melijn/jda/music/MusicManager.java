package me.melijn.jda.music;

import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import me.melijn.jda.Helpers;
import me.melijn.jda.commands.music.SPlayCommand;
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
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MusicManager {

    private final AudioPlayerManager manager = new DefaultAudioPlayerManager();
    private final Map<Long, MusicPlayer> players = new HashMap<>();
    private static MusicManager managerInstance = new MusicManager();
    public static HashMap<Long, List<AudioTrack>> userRequestedSongs = new HashMap<>();
    public static HashMap<Long, Long> userMessageToAnswer = new HashMap<>();

    public MusicManager() {
        manager.getConfiguration().setFilterHotSwapEnabled(true);
        manager.setFrameBufferDuration(1000);
        AudioSourceManagers.registerRemoteSources(manager);
        AudioSourceManagers.registerLocalSource(manager);
    }

    public static MusicManager getManagerInstance() {
        return managerInstance;
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
                eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
                eb.setFooter(Helpers.getFooterStamp(), null);
                eb.setColor(Helpers.EmbedColor);
                channel.sendMessage(eb.build()).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                if (tracks.size() == 0) {
                    channel.sendMessage("No songs found with this request '" + source.replaceFirst("ytsearch:|scsearch:", "") + "'").queue();
                    return;
                }
                if (!isPlaylist) {
                    trackLoaded(tracks.get(0));
                } else {
                    if (tracks.size() > 200)
                        tracks = tracks.subList(0, 200);
                    if (userRequestedSongs.get(requester.getIdLong()) == null && userMessageToAnswer.get(requester.getIdLong()) == null) {
                        userRequestedSongs.put(requester.getIdLong(), tracks);
                        StringBuilder songs = new StringBuilder();
                        for (AudioTrack track : tracks) {
                            songs.append(track.getInfo().title).append("\n");
                        }
                        String toSend = String.valueOf("You're about to add a playlist which contains these songs:\n" + songs + "Hit \u2705 to accept or \u274E to deny").length() < 2000 ?
                                "You're about to add a playlist which contains these songs:\n" + songs + "Hit \u2705 to accept or \u274E to deny" :
                                "You're about to add a playlist which contains " + tracks.size() + " songs.\nHit \u2705 to accept or \u274E to deny.";
                        channel.sendMessage(toSend).queue(message -> {
                            userMessageToAnswer.put(requester.getIdLong(), message.getIdLong());
                            message.addReaction("\u2705").queue();
                            message.addReaction("\u274E").queue();
                            Helpers.waitForIt(requester.getIdLong());
                            message.delete().queueAfter(30, TimeUnit.SECONDS, null, (failure) -> {
                            });
                        });
                    } else {
                        channel.sendMessage("You still have a request to answer. (request automatically gets removed after 30 seconds)")
                                .queue((message) -> message.delete().queueAfter(10, TimeUnit.SECONDS, null, (failure) -> {
                                }));
                    }
                }
            }

            @Override
            public void noMatches() {
                channel.sendMessage("I couldn't find a song called " + source + ". Check on spelling mistakes.").queue();
            }

            @Override
            public void loadFailed(FriendlyException ignored) {
                channel.sendMessage("Something went wrong while searching for your track").queue();
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
            public void loadFailed(FriendlyException ignored) {
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
                    if (i == 5) break;
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
                channel.sendMessage("I couldn't find a song named " + source + ". Check on spelling mistakes.").queue();
            }

            @Override
            public void loadFailed(FriendlyException ignored) {
                channel.sendMessage("Something went wrong while searching for your track").queue();
            }
        });
    }

    public void loadSpotifyTrack(TextChannel textChannel, User requester, String name, ArtistSimplified[] artists, int durationMs) {
        MusicPlayer player = getPlayer(textChannel.getGuild());
        textChannel.getGuild().getAudioManager().setSendingHandler(player.getAudioHandler());
        String title = name.replaceFirst("scsearch:|ytsearch:", "");
        ArrayList<String> artistNames = new ArrayList<>();
        StringBuilder source = new StringBuilder(name);
        if (artists != null) {
            if (artists.length > 0) source.append(" ");
            int i = 0;
            for (ArtistSimplified artistSimplified : artists) {
                artistNames.add(artistSimplified.getName());
                if (i++ == 0) source.append(artistSimplified.getName());
                else source.append(", ").append(artistSimplified.getName());
            }
        }
        manager.loadItemOrdered(player, source.toString(), new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                if ((durationMs + 2000 > track.getDuration() && track.getDuration() > durationMs - 2000) || track.getInfo().title.toLowerCase().contains(title.toLowerCase())) {
                    loadTrack(textChannel, source.toString(), requester, false);
                } else {
                    if (name.startsWith("ytsearch:"))
                        if (artists != null)
                            loadSpotifyTrack(textChannel, requester, name, null, durationMs);
                        else
                            loadSpotifyTrack(textChannel, requester, name.replaceFirst("ytsearch:", "scsearch:"), artists, durationMs);
                    else if (name.startsWith("scsearch:"))
                        if (artists != null)
                            loadSpotifyTrack(textChannel, requester, name, null, durationMs);
                        else textChannel.sendMessage("No track with that name found :/").queue();
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                for (AudioTrack track : tracks.subList(0, tracks.size() > 5 ? 5 : tracks.size())) {
                    if ((durationMs + 2000 > track.getDuration() && track.getDuration() > durationMs - 2000) || track.getInfo().title.toLowerCase().contains(title.toLowerCase())) {
                        loadTrack(textChannel, source.toString(), requester, false);
                        return;
                    }
                }
                if (name.startsWith("ytsearch:"))
                    if (artists != null)
                        loadSpotifyTrack(textChannel, requester, name, null, durationMs);
                    else
                        loadSpotifyTrack(textChannel, requester, name.replaceFirst("ytsearch:", "scsearch:"), artists, durationMs);
                else if (name.startsWith("scsearch:"))
                    if (artists != null)
                        loadSpotifyTrack(textChannel, requester, name, null, durationMs);
                    else textChannel.sendMessage("No track with that name found :(").queue();

            }

            @Override
            public void noMatches() {
                if (name.startsWith("ytsearch:"))
                    if (artists != null)
                        loadSpotifyTrack(textChannel, requester, name, null, durationMs);
                    else
                        loadSpotifyTrack(textChannel, requester, name.replaceFirst("ytsearch:", "scsearch:"), artists, durationMs);
                else if (name.startsWith("scsearch:"))
                    if (artists != null)
                        loadSpotifyTrack(textChannel, requester, name, null, durationMs);
                    else textChannel.sendMessage("No track with that name found :C").queue();
            }

            @Override
            public void loadFailed(FriendlyException ignored) {
                textChannel.sendMessage("Something went wrong while searching for your track").queue();
            }
        });
    }

    public void loadSpotifyPlaylist(TextChannel textChannel, PlaylistTrack[] tracks) {
        textChannel.sendMessage("I don't support playlists and albums yet").queue();
    }
}
