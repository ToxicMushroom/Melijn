package me.melijn.melijnbot.internals.music

import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import lavalink.client.player.IPlayer
import lavalink.client.player.LavaplayerPlayerWrapper
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer


class AudioPlayerSendHandler(iPlayer: IPlayer) : AudioSendHandler {

    private val audioPlayer = iPlayer as LavaplayerPlayerWrapper
    private val buffer = ByteBuffer.allocate(1024)
    private var lastFrame: MutableAudioFrame = MutableAudioFrame()

    init {
        lastFrame.setBuffer(buffer)
    }

    override fun canProvide(): Boolean {
        return audioPlayer.provide(lastFrame)
    }

    override fun provide20MsAudio(): ByteBuffer? {
        return buffer.flip()
    }

    override fun isOpus(): Boolean {
        return true
    }
}