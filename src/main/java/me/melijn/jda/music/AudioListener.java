package me.melijn.jda.music;

import me.melijn.jda.Helpers;
import me.melijn.jda.commands.music.LoopCommand;
import me.melijn.jda.commands.music.LoopQueueCommand;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.core.entities.Guild;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioListener extends AudioEventAdapter {

    public final BlockingQueue<AudioTrack> tracks = new LinkedBlockingQueue<>();
    private final MusicPlayer player;

    public AudioListener(MusicPlayer player) {
        this.player = player;
    }

    public BlockingQueue<AudioTrack> getTracks() {
        return tracks;
    }

    public int getTrackSize() {
        return tracks.size();
    }

    public void nextTrack(AudioTrack lastTrack) {
        if (tracks.isEmpty()) {
            if (player.getGuild().getAudioManager().getConnectedChannel() != null)
                Helpers.scheduleClose(player.getGuild().getAudioManager());
            return;
        }
        AudioTrack track = tracks.poll();
        if (track.equals(lastTrack))
            player.getAudioPlayer().startTrack(track.makeClone(), false);
        else player.getAudioPlayer().startTrack(track, false);
        Helpers.postMusicLog(player, track);
    }

    @Override
    public void onTrackEnd(AudioPlayer player2, AudioTrack track, AudioTrackEndReason endReason) {
        Guild guild = player.getGuild();
        if (LoopCommand.looped.contains(guild.getIdLong())) {
            MusicManager.getManagerInstance().loadSimpelTrack(player.getGuild(), track.getInfo().uri);
        } else if (LoopQueueCommand.looped.contains(guild.getIdLong())) {
            if (endReason.mayStartNext) nextTrack(track);
            MusicManager.getManagerInstance().loadSimpelTrack(player.getGuild(), track.getInfo().uri);
        } else {
            if (endReason.mayStartNext) nextTrack(track);
        }
    }

    public void queue(AudioTrack track) {
        boolean success = player.getAudioPlayer().startTrack(track, true);
        if (!success) tracks.offer(track);
        else Helpers.postMusicLog(player, track);
    }
}
