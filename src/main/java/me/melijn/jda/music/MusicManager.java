package me.melijn.jda.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.TrackSimplified;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import me.melijn.jda.Helpers;
import me.melijn.jda.commands.music.SPlayCommand;
import me.melijn.jda.utils.Embedder;
import me.melijn.jda.utils.YTSearch;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MusicManager {

    private final AudioPlayerManager manager = new DefaultAudioPlayerManager();
    private final TLongObjectMap<MusicPlayer> players = new TLongObjectHashMap<>();
    private static MusicManager managerInstance = new MusicManager();
    public static TLongObjectMap<List<AudioTrack>> userRequestedSongs = new TLongObjectHashMap<>();
    public static TLongLongMap userMessageToAnswer = new TLongLongHashMap();
    private static final YTSearch ytSearch = new YTSearch();
    private final static String youtubeVideoBase = "https://youtu.be/";

    public MusicManager() {
        manager.getConfiguration().setFilterHotSwapEnabled(true);
        manager.setFrameBufferDuration(1000);
        AudioSourceManagers.registerRemoteSources(manager);
        AudioSourceManagers.registerLocalSource(manager);
    }

    public TLongObjectMap<MusicPlayer> getPlayers() {
        return players;
    }

    public static MusicManager getManagerInstance() {
        return managerInstance;
    }

    public synchronized MusicPlayer getPlayer(Guild guild) {
        if (!players.containsKey(guild.getIdLong()))
            players.put(guild.getIdLong(), new MusicPlayer(manager.createPlayer(), guild));
        return players.get(guild.getIdLong());
    }


    /*
      source/identifier:
        Prefix with ytsearch: to search youtube.
        Prefix with scsearch: to search soundcloud

        Pass a URL to load playlists/tracks
        Pass the path of a file to load local track (i.e. /home/user/myfile.mp3 or C:\path\to\myfile.mp3)
     */
    public void loadTrack(final TextChannel channel, final String source, User requester, boolean isPlaylist) {
        MusicPlayer player = getPlayer(channel.getGuild());
        channel.getGuild().getAudioManager().setSendingHandler(player.getAudioHandler());
        AudioLoadResultHandler resultHandler = new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                iHateDuplicates(track, player, channel);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                if (tracks.size() == 0) {
                    boolean prefixed = source.startsWith("ytsearch:") || source.startsWith("scsearch:");
                    channel.sendMessage("No songs found with this request '" + (prefixed ? source.replaceFirst("ytsearch:|scsearch:", "") : source) + "'").queue();
                    return;
                }
                if (!isPlaylist) {
                    trackLoaded(tracks.get(0));
                } else {
                    if (tracks.size() > 200)
                        tracks = tracks.subList(0, 200);
                    if (!userRequestedSongs.containsKey(requester.getIdLong()) && !userMessageToAnswer.containsKey(requester.getIdLong())) {
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
                channel.sendMessage("I couldn't find a song called " + source + ". Check for spelling mistakes.").queue();
            }

            @Override
            public void loadFailed(FriendlyException ignored) {
                channel.sendMessage("Something went wrong while searching for your track").queue();
            }
        };
        String url = source;
        if (url.startsWith("ytsearch:")) {
            String result = ytSearch.search(url.replaceFirst("ytsearch:", ""));
            if (result == null) {
                channel.sendMessage("I couldn't find a song called " + source.replaceFirst("ytsearch:", "") + ". Check for spelling mistakes.").queue();
                return;
            }
            url = youtubeVideoBase + result;
        }
        manager.loadItemOrdered(player, url, resultHandler);
    }

    public void loadSimpleTrack(Guild guild, final String source) {
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
                if (tracks.size() < 1) return;
                player.playTrack(tracks.get(0));
            }

            @Override
            public void noMatches() {}

            @Override
            public void loadFailed(FriendlyException ignored) {}
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
                EmbedBuilder eb = new Embedder(channel.getGuild());
                eb.setTitle("Select Menu");
                eb.setFooter(Helpers.getFooterStamp(), null);
                StringBuilder sb = new StringBuilder();
                TIntObjectMap<AudioTrack> map = new TIntObjectHashMap<>();
                int i = 0;
                for (AudioTrack track : tracks) {
                    map.put(i, track);
                    if (i == 5) break;
                    sb.append("[").append(++i).append("](").append(track.getInfo().uri).append(") - ").append(track.getInfo().title).append(" `[").append(Helpers.getDurationBreakdown(track.getInfo().length)).append("]`\n");
                }
                eb.setDescription(sb.toString());
                SPlayCommand.userChoices.put(author.getIdLong(), map);
                channel.sendMessage(eb.build()).queue((s) -> {
                    SPlayCommand.usersFormToReply.put(author.getIdLong(), s);
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
                channel.sendMessage("I couldn't find songs named " + source + ". Check for spelling mistakes.").queue();
            }

            @Override
            public void loadFailed(FriendlyException ignored) {
                channel.sendMessage("Something went wrong while searching for your track").queue();
            }
        });
    }

    public void loadSpotifyTrack(TextChannel textChannel, String name, ArtistSimplified[] artists, int durationMs) {
        MusicPlayer player = getPlayer(textChannel.getGuild());
        textChannel.getGuild().getAudioManager().setSendingHandler(player.getAudioHandler());
        String title = name.replaceFirst("scsearch:|ytsearch:", "");
        StringBuilder source = new StringBuilder(name);
        List<String> artistNames = new ArrayList<>();
        appendArtists(artists, source, artistNames);
        manager.loadItemOrdered(player, source.toString(), new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                if ((durationMs + 2000 > track.getDuration() && track.getDuration() > durationMs - 2000) || track.getInfo().title.toLowerCase().contains(title.toLowerCase())) {
                    iHateDuplicates(track, player, textChannel);
                } else {
                    if (name.startsWith("ytsearch:"))
                        if (artists != null)
                            loadSpotifyTrack(textChannel, name, null, durationMs);
                        else
                            loadSpotifyTrack(textChannel, name.replaceFirst("ytsearch:", "scsearch:"), artists, durationMs);
                    else if (name.startsWith("scsearch:"))
                        if (artists != null)
                            loadSpotifyTrack(textChannel, name, null, durationMs);
                        else textChannel.sendMessage("No track with that name found :/").queue();
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                for (AudioTrack track : tracks.subList(0, tracks.size() > 5 ? 5 : tracks.size())) {
                    if ((durationMs + 2000 > track.getDuration() && track.getDuration() > durationMs - 2000) || track.getInfo().title.toLowerCase().contains(title.toLowerCase())) {
                        player.playTrack(track);
                        EmbedBuilder eb = new Embedder(textChannel.getGuild());
                        eb.setTitle("Added");
                        eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
                        eb.setFooter(Helpers.getFooterStamp(), null);
                        textChannel.sendMessage(eb.build()).queue();
                        return;
                    }
                }
                if (name.startsWith("ytsearch:"))
                    if (artists != null)
                        loadSpotifyTrack(textChannel, name, null, durationMs);
                    else
                        loadSpotifyTrack(textChannel, name.replaceFirst("ytsearch:", "scsearch:"), artists, durationMs);
                else if (name.startsWith("scsearch:"))
                    if (artists != null)
                        loadSpotifyTrack(textChannel, name, null, durationMs);
                    else textChannel.sendMessage("No track with that name found :(").queue();

            }

            @Override
            public void noMatches() {
                if (name.startsWith("ytsearch:"))
                    if (artists != null)
                        loadSpotifyTrack(textChannel, name, null, durationMs);
                    else
                        loadSpotifyTrack(textChannel, name.replaceFirst("ytsearch:", "scsearch:"), artists, durationMs);
                else if (name.startsWith("scsearch:"))
                    if (artists != null)
                        loadSpotifyTrack(textChannel, name, null, durationMs);
                    else textChannel.sendMessage("No track with that name found :C").queue();
            }

            @Override
            public void loadFailed(FriendlyException ignored) {
                textChannel.sendMessage("Something went wrong while searching for your track").queue();
            }
        });
    }

    private void iHateDuplicates(AudioTrack track, MusicPlayer player, TextChannel textChannel) {
        player.playTrack(track);
        EmbedBuilder eb = new Embedder(player.getGuild());
        eb.setTitle("Added");
        eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
        eb.setFooter(Helpers.getFooterStamp(), null);
        textChannel.sendMessage(eb.build()).queue();
    }

    private void loadSpotifyTracks(Guild guild, String name, ArtistSimplified[] artists, ArtistSimplified[] unused, int durationMs) {
        MusicPlayer player = getPlayer(guild);
        guild.getAudioManager().setSendingHandler(player.getAudioHandler());
        String title = name.replaceFirst("scsearch:|ytsearch:", "");
        StringBuilder source = new StringBuilder(name);
        List<String> artistNames = new ArrayList<>();
        appendArtists(artists, source, artistNames);
        manager.loadItemOrdered(player, source.toString(), new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                if ((durationMs + 2000 > track.getDuration() && track.getDuration() > durationMs - 2000) ||
                        (track.getInfo().title.toLowerCase().contains(title.toLowerCase()) && (durationMs + 60000 > track.getDuration() && track.getDuration() > durationMs - 60000))) {
                    player.playTrack(track);
                } else {
                    tryAgain(name, artists, unused, guild, durationMs);
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                for (AudioTrack track : tracks.subList(0, tracks.size() > 5 ? 5 : tracks.size())) {
                    if ((durationMs + 2000 > track.getDuration() && track.getDuration() > durationMs - 2000) ||
                            (track.getInfo().title.toLowerCase().contains(title.toLowerCase()) && (durationMs + 60000 > track.getDuration() && track.getDuration() > durationMs - 60000))) {
                        player.playTrack(track);
                        return;
                    }
                }
                tryAgain(name, artists, unused, guild, durationMs);

            }

            @Override
            public void noMatches() {
                tryAgain(name, artists, unused, guild, durationMs);
            }

            @Override
            public void loadFailed(FriendlyException ignored) {
            }
        });
    }

    private void appendArtists(ArtistSimplified[] artists, StringBuilder source, List<String> artistNames) {
        if (artists != null) {
            if (artists.length > 0) source.append(" ");
            int i = 0;
            for (ArtistSimplified artistSimplified : artists) {
                artistNames.add(artistSimplified.getName());
                if (i++ == 0) source.append(artistSimplified.getName());
                else source.append(", ").append(artistSimplified.getName());
            }
        }
    }

    private void tryAgain(String name, ArtistSimplified[] artists, ArtistSimplified[] unused, Guild guild, int durationMs) {
        if (name.startsWith("ytsearch:"))
            if (artists == null)
                loadSpotifyTracks(guild, name.replaceFirst("ytsearch:", "scsearch:"), unused, unused, durationMs);
            else
                loadSpotifyTracks(guild, name, null, unused, durationMs);
        else if (name.startsWith("scsearch:") && artists != null)
            loadSpotifyTracks(guild, name, null, unused, durationMs);
    }

    public void loadSpotifyPlaylist(TextChannel textChannel, PlaylistTrack[] tracks) {
        if (tracks.length > 40) {
            textChannel.sendMessage("Sorry but I only support playlists up to 40 songs").queue();
            return;
        }

        for (PlaylistTrack track : tracks) {
            loadSpotifyTracks(textChannel.getGuild(), "ytsearch:" + track.getTrack().getName(), track.getTrack().getArtists(), track.getTrack().getArtists(), track.getTrack().getDurationMs());
        }
        textChannel.sendMessage("Adding " + tracks.length + " song" + (tracks.length > 1 ? "s" : "") + " to the queue").queue();
    }

    public void loadSpotifyAlbum(TextChannel textChannel, TrackSimplified[] tracksa) {
        if (tracksa.length > 40) {
            textChannel.sendMessage("Sorry but I only support albums up to 40 songs").queue();
            return;
        }
        for (TrackSimplified track : tracksa) {
            loadSpotifyTracks(textChannel.getGuild(), "ytsearch:" + track.getName(), track.getArtists(), track.getArtists(), track.getDurationMs());
        }
        textChannel.sendMessage("Adding " + tracksa.length + " song" + (tracksa.length > 1 ? "s" : "") + " to the queue").queue();
    }
}
