package com.pixelatedsource.jda.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.commands.music.LoopCommand;
import com.pixelatedsource.jda.commands.music.LoopQueueCommand;
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

    public void nextTrack() {
        if (tracks.isEmpty()) {
            if (player.getGuild().getAudioManager().getConnectedChannel() != null)
                Helpers.ScheduleClose(player.getGuild().getAudioManager());
            return;
        }
        player.getAudioPlayer().startTrack(tracks.poll(), false);
    }

    @Override
    public void onTrackEnd(AudioPlayer player2, AudioTrack track, AudioTrackEndReason endReason) {
        Guild guild = player.getGuild();
        if (LoopCommand.looped.getOrDefault(guild.getIdLong(), false)) {
            MusicManager.getManagerinstance().loadSimpelTrack(player.getGuild(), track.getInfo().uri);
        } else if (LoopQueueCommand.looped.getOrDefault(guild.getIdLong(), false)) {
            if (endReason.mayStartNext) nextTrack();
            MusicManager.getManagerinstance().loadSimpelTrack(player.getGuild(), track.getInfo().uri);
        } else {
            if (endReason.mayStartNext) nextTrack();
        }
    }

    public void queue(AudioTrack track) {
        if (!player.getAudioPlayer().startTrack(track, true)) tracks.offer(track);
    }
}
