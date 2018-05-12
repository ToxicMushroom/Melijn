package com.pixelatedsource.jda.music;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.commands.music.LoopCommand;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

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
        if (LoopCommand.looped.get(player.getGuild()) == null || !LoopCommand.looped.get(player.getGuild())) {
            if (endReason.mayStartNext) nextTrack();
        } else if (LoopCommand.looped.get(player.getGuild())) {
            MusicManager.getManagerinstance().loadSimpelTrack(player.getGuild(), track.getInfo().uri);
        }
    }

    public void queue(AudioTrack track) {
        if (!player.getAudioPlayer().startTrack(track, true)) tracks.offer(track);
    }
}
