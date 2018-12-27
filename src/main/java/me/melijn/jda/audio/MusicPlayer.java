package me.melijn.jda.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.player.LavalinkPlayer;

public class MusicPlayer {

    private final LavalinkPlayer player;
    private final TrackManager manager;
    private final Lava lava = Lava.lava;
    private final long guildId;

    private final AudioSendingHandler sendHandler;

    public MusicPlayer(long guildId) {
        this.player = Lava.lava.createPlayer(guildId);
        this.guildId = guildId;
        manager = new TrackManager(player, this);
        sendHandler = new AudioSendingHandler(player);
    }

    public LavalinkPlayer getAudioPlayer() {
        return player;
    }

    public long getGuildId() {
        return guildId;
    }

    public TrackManager getTrackManager() {
        return manager;
    }

    public AudioSendingHandler getAudioHandler() {
        return sendHandler;
    }

    public synchronized void queue(AudioTrack track) {
        manager.queue(track);
    }

    public synchronized void resumeTrack() {
        player.setPaused(false);
    }

    public synchronized void stopTrack() {
        player.stopTrack();
        lava.getLavalink().getLink(String.valueOf(guildId)).disconnect();
    }

    public synchronized void skipTrack() {
        player.stopTrack();
        manager.nextTrack(getAudioPlayer().getPlayingTrack());
    }

    public synchronized boolean isPaused() {
        return player.isPaused();
    }

}
