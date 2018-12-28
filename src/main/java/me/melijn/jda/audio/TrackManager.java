package me.melijn.jda.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lavalink.client.player.LavalinkPlayer;
import lavalink.client.player.event.AudioEventAdapterWrapped;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.commands.music.LoopCommand;
import me.melijn.jda.commands.music.LoopQueueCommand;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TrackManager extends AudioEventAdapterWrapped {

    public final Queue<AudioTrack> tracks = new LinkedList<>();
    private final LavalinkPlayer player;
    private final MusicPlayer musicPlayer;

    public TrackManager(LavalinkPlayer player, MusicPlayer musicPlayer) {
        this.player = player;
        this.musicPlayer = musicPlayer;
    }

    public Queue<AudioTrack> getTracks() {
        return tracks;
    }

    public int getTrackSize() {
        return tracks.size();
    }

    public void nextTrack(AudioTrack lastTrack) {
        if (tracks.isEmpty()) {
            player.getLink().disconnect();
            return;
        }
        AudioTrack track = tracks.poll();
        if (track.equals(lastTrack)) player.playTrack(track.makeClone());
        else player.playTrack(track);
    }

    public void queue(AudioTrack track) {
        if (player.getPlayingTrack() == null) {
            player.playTrack(track);
        } else {
            tracks.offer(track);
        }
    }

    public void setSendingHandler(long guildId) {
        Guild guild = Melijn.getShardManager().getGuildById(guildId);
        guild.getAudioManager().setSendingHandler(musicPlayer.getAudioHandler());
    }

    public void shuffle() {
        Collections.shuffle((List<?>) tracks);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        Helpers.postMusicLog(musicPlayer.getGuildId(), track);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        long guildId = this.player.getLink().getGuildIdLong();
        if (LoopCommand.looped.contains(guildId)) {
            AudioLoader.getManagerInstance().loadSimpleTrack(this.musicPlayer, track.getInfo().uri);
        } else if (LoopQueueCommand.looped.contains(guildId)) {
            if (endReason.mayStartNext) nextTrack(track);
            AudioLoader.getManagerInstance().loadSimpleTrack(this.musicPlayer, track.getInfo().uri);
        } else {
            if (endReason.mayStartNext) nextTrack(track);
        }
    }

    public void clear() {
        tracks.clear();
    }
}
