package me.melijn.jda.audio;

import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import lavalink.client.player.IPlayer;
import lavalink.client.player.LavaplayerPlayerWrapper;
import net.dv8tion.jda.core.audio.AudioSendHandler;

public class AudioSendingHandler implements AudioSendHandler {


    private final IPlayer audioPlayer;
    private AudioFrame lastFrame;

    public AudioSendingHandler(IPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
    }

    @Override
    public boolean canProvide() {
        if (lastFrame == null) lastFrame = ((LavaplayerPlayerWrapper) audioPlayer).provide();
        return lastFrame != null;
    }

    @Override
    public byte[] provide20MsAudio() {
        byte[] data = canProvide() ? lastFrame.getData() : null;
        lastFrame = null;

        return data;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
