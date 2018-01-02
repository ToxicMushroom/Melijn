package com.pixelatedsource.jda.music;

import com.pixelatedsource.jda.Helpers;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Guild;

public class MusicPlayer {

    private final AudioPlayer audioPlayer;
    private final AudioListener listener;
    private final Guild guild;

    public MusicPlayer(AudioPlayer audioPlayer, Guild guild) {
        this.audioPlayer = audioPlayer;
        this.guild = guild;
        listener = new AudioListener(this);
        audioPlayer.addListener(listener);
        audioPlayer.setVolume(69);
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public Guild getGuild() {
        return guild;
    }

    public AudioListener getListener() {
        return listener;
    }

    public AudioHandler getAudioHandler() {
        return new AudioHandler(audioPlayer);
    }

    public synchronized void playTrack(AudioTrack track) {
        listener.queue(track);
    }

    public synchronized void resumeTrack() {
        audioPlayer.setPaused(false);
    }

    public synchronized void stopTrack() {
        audioPlayer.stopTrack();
        Helpers.ScheduleClose(guild.getAudioManager());
    }

    public synchronized void skipTrack() {
        audioPlayer.stopTrack();
        listener.nextTrack();
    }

    public synchronized boolean getPaused() {
        return audioPlayer.isPaused();
    }

}
