package me.melijn.jda.music;

import com.github.natanbc.lavadsp.chain.ChainedFilterBuilder;
import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.github.natanbc.lavadsp.tremolo.TremoloPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import me.melijn.jda.Helpers;
import net.dv8tion.jda.core.entities.Guild;

public class MusicPlayer {

    private final AudioPlayer audioPlayer;
    private final AudioListener listener;
    private final Guild guild;
    private TIntDoubleMap filters = new TIntDoubleHashMap();

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
        filters.put(1, 0D); //Depth (cannot be disabled without removing the whole effect class from the builder)
        filters.put(2, 0D); //Frequency (cannot be disabled without removing the whole effect class from the builder)
        filters.put(3, 1D); //speed default 1
        filters.put(4, 1D); //pitch default 1
    }

    public double getDepth() {
        return filters.get(1);
    }

    public double getFrequency() {
        return filters.get(2);
    }

    public double getSpeed() {
        return filters.get(3);
    }

    public double getPitch() {
        return filters.get(4);
    }

    public void setDepth(double value) {
        double temp = value;
        if (temp == 1) temp = 0.999D;
        filters.put(1, temp);
    }

    public void setFrequency(double value) {
        filters.put(2, value);
    }

    public void setSpeed(double value) {
        filters.put(3, value);
    }

    public void setPitch(double value) {
        filters.put(4, value);
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
        Helpers.scheduleClose(guild.getAudioManager());
    }

    public synchronized void skipTrack() {
        audioPlayer.stopTrack();
        listener.nextTrack(getAudioPlayer().getPlayingTrack());
    }

    public synchronized boolean getPaused() {
        return audioPlayer.isPaused();
    }

}
