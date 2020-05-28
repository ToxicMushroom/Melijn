package me.melijn.melijnbot.objects.music.custom

import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import java.util.concurrent.TimeUnit

class MelijnAudioPlayer(melijnAudioPlayerManager: MelijnAudioPlayerManager) : AudioPlayer {
    override fun startTrack(track: AudioTrack?, noInterrupt: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun checkCleanup(threshold: Long) {
        TODO("Not yet implemented")
    }

    override fun playTrack(track: AudioTrack?) {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

    override fun addListener(listener: AudioEventListener?) {
        TODO("Not yet implemented")
    }

    override fun getPlayingTrack(): AudioTrack {
        TODO("Not yet implemented")
    }

    override fun getVolume(): Int {
        TODO("Not yet implemented")
    }

    override fun stopTrack() {
        TODO("Not yet implemented")
    }

    override fun removeListener(listener: AudioEventListener?) {
        TODO("Not yet implemented")
    }

    override fun setFrameBufferDuration(duration: Int?) {
        TODO("Not yet implemented")
    }

    override fun setVolume(volume: Int) {
        TODO("Not yet implemented")
    }

    override fun setFilterFactory(factory: PcmFilterFactory?) {
        TODO("Not yet implemented")
    }

    override fun setPaused(value: Boolean) {
        TODO("Not yet implemented")
    }

    override fun provide(): AudioFrame {
        TODO("Not yet implemented")
    }

    override fun provide(timeout: Long, unit: TimeUnit?): AudioFrame {
        TODO("Not yet implemented")
    }

    override fun provide(targetFrame: MutableAudioFrame?): Boolean {
        TODO("Not yet implemented")
    }

    override fun provide(targetFrame: MutableAudioFrame?, timeout: Long, unit: TimeUnit?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isPaused(): Boolean {
        TODO("Not yet implemented")
    }
}