package me.melijn.jda.music;

import com.github.natanbc.lavadsp.chain.ChainedFilterBuilder;
import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.github.natanbc.lavadsp.tremolo.TremoloPcmAudioFilter;
import me.melijn.jda.Helpers;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

public class MusicPlayer {

    private final AudioPlayer audioPlayer;
    private final AudioListener listener;
    private final Guild guild;
    private HashMap<Long, Double> filters = new HashMap<>();

    public void updateFilters() {
        ChainedFilterBuilder builder = new ChainedFilterBuilder();
        boolean timeEnabled = false;
        boolean tremoloEnabled = false;
        if (getSpeed() > 0 && getPitch() > 0) {
            builder.add(TimescalePcmAudioFilter::new);
            builder.addConfigurator(TimescalePcmAudioFilter.class, timescalePcmAudioFilter -> {
                timescalePcmAudioFilter.setPitch(getPitch());
                timescalePcmAudioFilter.setSpeed(getSpeed());
            });
            timeEnabled = true;
        }
        if (getFrequency() > 0 && getDepth() > 0 && getDepth() < 1) {
            builder.add(TremoloPcmAudioFilter::new);
            builder.addConfigurator(TremoloPcmAudioFilter.class, tremoloPcmAudioFilter -> {
                tremoloPcmAudioFilter.setDepth(getDepth());
                tremoloPcmAudioFilter.setFrequency(getFrequency());
            });
            tremoloEnabled = true;
        }
        if (tremoloEnabled || timeEnabled) {
            audioPlayer.setFilterFactory(builder);
        }
    }

    public MusicPlayer(AudioPlayer audioPlayer, Guild guild) {
        this.audioPlayer = audioPlayer;
        this.guild = guild;
        listener = new AudioListener(this);
        this.audioPlayer.addListener(listener);
        this.audioPlayer.setVolume(100);
        filters.put(1L, 0D); //Depth (cannot be disabled without removing the whole effect class from the builder)
        filters.put(2L, 0D); //Frequency (cannot be disabled without removing the whole effect class from the builder)
        filters.put(3L, 1D); //speed default 1
        filters.put(4L, 1D); //pitch default 1
    }

    public double getDepth() {
        return filters.get(1L);
    }

    public double getFrequency() {
        return filters.get(2L);
    }

    public double getSpeed() {
        return filters.get(3L);
    }

    public double getPitch() {
        return filters.get(4L);
    }

    public void setDepth(double value) {
        if (value == 1) value = 0.999D;
        filters.replace(1L, value);
    }

    public void setFrequency(double value) {
        filters.replace(2L, value);
    }

    public void setSpeed(double value) {
        filters.replace(3L, value);
    }

    public void setPitch(double value) {
        filters.replace(4L, value);
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
        listener.nextTrack(getAudioPlayer().getPlayingTrack());
    }

    public synchronized boolean getPaused() {
        return audioPlayer.isPaused();
    }

}
