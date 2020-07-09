package me.melijn.melijnbot.internals.music.lavaimpl

import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerOptions
import com.sedmelluq.discord.lavaplayer.player.event.*
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack
import com.sedmelluq.discord.lavaplayer.track.TrackStateListener
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameProviderTools
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An audio player that is capable of playing audio tracks and provides audio frames from the currently playing track.
 */

/**
 * @param manager Audio player manager which this player is attached to
 */
class MelijnAudioPlayer(private val manager: MelijnAudioPlayerManager) : AudioPlayer, TrackStateListener {
    @Volatile
    private var activeTrack: InternalAudioTrack? = null

    @Volatile
    private var lastRequestTime: Long = 0

    @Volatile
    private var lastReceiveTime: Long = 0

    @Volatile
    private var stuckEventSent = false

    @Volatile
    private var shadowTrack: InternalAudioTrack? = null
    private val paused: AtomicBoolean = AtomicBoolean()
    private val listeners: MutableList<AudioEventListener>
    private val trackSwitchLock: Any
    private val options: AudioPlayerOptions

    /**
     * @return Currently playing track
     */
    override fun getPlayingTrack(): AudioTrack? {
        return activeTrack
    }

    /**
     * @param track The track to start playing
     */
    override fun playTrack(track: AudioTrack) {
        startTrack(track, false)
    }

    /**
     * @param track The track to start playing, passing null will stop the current track and return false
     * @param noInterrupt Whether to only start if nothing else is playing
     * @return True if the track was started
     */
    override fun startTrack(track: AudioTrack?, noInterrupt: Boolean): Boolean {
        val newTrack = track as InternalAudioTrack?
        var previousTrack: InternalAudioTrack?
        synchronized(trackSwitchLock) {
            previousTrack = activeTrack
            if (noInterrupt && previousTrack != null) {
                return false
            }
            activeTrack = newTrack
            lastRequestTime = System.currentTimeMillis()
            lastReceiveTime = System.nanoTime()
            stuckEventSent = false
            if (previousTrack != null) {
                previousTrack?.stop()
                dispatchEvent(TrackEndEvent(this, previousTrack, if (newTrack == null) AudioTrackEndReason.STOPPED else AudioTrackEndReason.REPLACED))
                shadowTrack = previousTrack
            }
        }
        if (newTrack == null) {
            shadowTrack = null
            return false
        }
        dispatchEvent(TrackStartEvent(this, newTrack))
        manager.executeTrack(this, newTrack, manager.configuration, options)
        return true
    }

    /**
     * Stop currently playing track.
     */
    override fun stopTrack() {
        stopWithReason(AudioTrackEndReason.STOPPED)
    }

    private fun stopWithReason(reason: AudioTrackEndReason) {
        shadowTrack = null
        synchronized(trackSwitchLock) {
            val previousTrack = activeTrack
            activeTrack = null
            if (previousTrack != null) {
                previousTrack.stop()
                dispatchEvent(TrackEndEvent(this, previousTrack, reason))
            }
        }
    }

    private fun provideShadowFrame(): AudioFrame? {
        val shadow = shadowTrack
        var frame: AudioFrame? = null
        if (shadow != null) {
            frame = shadow.provide()
            if (frame != null && frame.isTerminator) {
                shadowTrack = null
                frame = null
            }
        }
        return frame
    }

    private fun provideShadowFrame(targetFrame: MutableAudioFrame): Boolean {
        val shadow = shadowTrack
        if (shadow != null && shadow.provide(targetFrame)) {
            if (targetFrame.isTerminator) {
                shadowTrack = null
                return false
            }
            return true
        }
        return false
    }

    override fun provide(): AudioFrame {
        return AudioFrameProviderTools.delegateToTimedProvide(this)
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    override fun provide(timeout: Long, unit: TimeUnit): AudioFrame? {
        lastRequestTime = System.currentTimeMillis()
        if (timeout == 0L && paused.get()) {
            return null
        }
        while (activeTrack != null) {
            val track = activeTrack ?: continue
            var frame = if (timeout > 0) track.provide(timeout, unit) else track.provide()
            if (frame != null) {
                lastReceiveTime = System.nanoTime()
                shadowTrack = null
                if (frame.isTerminator) {
                    handleTerminator(track)
                    continue
                }
            } else if (timeout == 0L) {
                checkStuck(track)
                frame = (provideShadowFrame() ?: return null)
            }
            return frame
        }
        return null
    }

    override fun provide(targetFrame: MutableAudioFrame): Boolean {
        return try {
            provide(targetFrame, 0, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            ExceptionTools.keepInterrupted(e)
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            ExceptionTools.keepInterrupted(e)
            throw RuntimeException(e)
        }
    }

    @Throws(TimeoutException::class, InterruptedException::class)
    override fun provide(targetFrame: MutableAudioFrame, timeout: Long, unit: TimeUnit): Boolean {
        lastRequestTime = System.currentTimeMillis()
        if (timeout == 0L && paused.get()) {
            return false
        }
        while (activeTrack != null) {
            val track = activeTrack ?: continue
            return if (if (timeout > 0) track.provide(targetFrame, timeout, unit) else track.provide(targetFrame)) {
                lastReceiveTime = System.nanoTime()
                shadowTrack = null
                if (targetFrame.isTerminator) {
                    handleTerminator(track)
                    continue
                }
                true
            } else if (timeout == 0L) {
                checkStuck(track)
                provideShadowFrame(targetFrame)
            } else {
                false
            }
        }
        return false
    }

    private fun handleTerminator(track: InternalAudioTrack) {
        synchronized(trackSwitchLock) {
            if (activeTrack === track) {
                activeTrack = null
                dispatchEvent(TrackEndEvent(this, track, if (track.activeExecutor.failedBeforeLoad()) AudioTrackEndReason.LOAD_FAILED else AudioTrackEndReason.FINISHED))
            }
        }
    }

    private fun checkStuck(track: AudioTrack) {
        if (!stuckEventSent && System.nanoTime() - lastReceiveTime > manager.trackStuckThresholdNanos) {
            stuckEventSent = true
            val stackTrace = getStackTrace(track)
            val threshold = TimeUnit.NANOSECONDS.toMillis(manager.trackStuckThresholdNanos)
            dispatchEvent(TrackStuckEvent(this, track, threshold, stackTrace))
        }
    }

    private fun getStackTrace(track: AudioTrack): Array<StackTraceElement>? {
        if (track is InternalAudioTrack) {
            val executor = track.activeExecutor
            if (executor is LocalAudioTrackExecutor) {
                return executor.stackTrace
            }
        }
        return null
    }

    override fun getVolume(): Int {
        return options.volumeLevel.get()
    }

    override fun setVolume(volume: Int) {
        options.volumeLevel.set(1000.coerceAtMost(0.coerceAtLeast(volume)))
    }

    override fun setFilterFactory(factory: PcmFilterFactory) {
        options.filterFactory.set(factory)
    }

    override fun setFrameBufferDuration(duration: Int) {
        var durationShadow: Int? = duration
        if (durationShadow != null) {
            durationShadow = 200.coerceAtLeast(durationShadow)
        }
        options.frameBufferDuration.set(durationShadow)
    }

    /**
     * @return Whether the player is paused
     */
    override fun isPaused(): Boolean {
        return paused.get()
    }

    /**
     * @param value True to pause, false to resume
     */
    override fun setPaused(value: Boolean) {
        if (paused.compareAndSet(!value, value)) {
            if (value) {
                dispatchEvent(PlayerPauseEvent(this))
            } else {
                dispatchEvent(PlayerResumeEvent(this))
                lastReceiveTime = System.nanoTime()
            }
        }
    }

    /**
     * Destroy the player and stop playing track.
     */
    override fun destroy() {
        stopTrack()
    }

    /**
     * Add a listener to events from this player.
     * @param listener New listener
     */
    override fun addListener(listener: AudioEventListener) {
        synchronized(trackSwitchLock) { listeners.add(listener) }
    }

    /**
     * Remove an attached listener using identity comparison.
     * @param listener The listener to remove
     */
    override fun removeListener(listener: AudioEventListener) {
        synchronized(trackSwitchLock) {
            val iterator = listeners.iterator()
            while (iterator.hasNext()) {
                if (iterator.next() === listener) {
                    iterator.remove()
                }
            }
        }
    }

    private fun dispatchEvent(event: AudioEvent) {
        log.debug("Firing an event with class {}", event.javaClass.simpleName)
        synchronized(trackSwitchLock) {
            for (listener in listeners) {
                try {
                    listener.onEvent(event)
                } catch (e: Exception) {
                    log.error("Handler of event {} threw an exception.", event, e)
                }
            }
        }
    }

    override fun onTrackException(track: AudioTrack, exception: FriendlyException) {
        dispatchEvent(TrackExceptionEvent(this, track, exception))
    }

    override fun onTrackStuck(track: AudioTrack, thresholdMs: Long) {
        dispatchEvent(TrackStuckEvent(this, track, thresholdMs, null))
    }

    /**
     * Check if the player should be "cleaned up" - stopped due to nothing using it, with the given threshold.
     * @param threshold Threshold in milliseconds to use
     */
    override fun checkCleanup(threshold: Long) {
        val track = playingTrack
        if (track != null && System.currentTimeMillis() - lastRequestTime >= threshold) {
            log.debug("Triggering cleanup on an audio player playing track {}", track)
            stopWithReason(AudioTrackEndReason.CLEANUP)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AudioPlayer::class.java)
    }


    init {
        listeners = ArrayList()
        trackSwitchLock = Any()
        options = AudioPlayerOptions()
    }
}