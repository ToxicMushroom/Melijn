package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import lavalink.client.player.IPlayer
import lavalink.client.player.event.AudioEventAdapterWrapped
import java.util.*


class GuildTrackManager(
    val iPlayer: IPlayer
) : AudioEventAdapterWrapped() {

    var loopedTrack = false
    var loopedQueue = false

    var tracks: Queue<AudioTrack> = LinkedList()
    fun trackSize() = tracks.size


    fun nextTrack(lastTrack: AudioTrack) {
        if (tracks.isEmpty()) {
            if (loopedQueue || loopedTrack) {
                iPlayer.playTrack(lastTrack.makeClone())
                return
            }
            iPlayer.stopTrack()
            return
        }

        if (loopedTrack) {
            iPlayer.playTrack(lastTrack.makeClone())
            return
        }

        val track: AudioTrack = tracks.poll()
        if (track == lastTrack) iPlayer.playTrack(track.makeClone()) else iPlayer.playTrack(track)
        if (loopedQueue) {
            tracks.add(lastTrack)
        }
    }

    fun queue(track: AudioTrack) {
        if (iPlayer.playingTrack == null) {
            iPlayer.playTrack(track)
        } else {
            tracks.offer(track)
        }
    }

    fun shuffle() {
        tracks = LinkedList(tracks.shuffled())
    }


    override fun onPlayerResume(player: AudioPlayer?) {
        println("track resume")
    }

    override fun onPlayerPause(player: AudioPlayer?) {
        println("track paused")
    }

    override fun onTrackStuck(player: AudioPlayer?, track: AudioTrack?, thresholdMs: Long) {
        println("track stuck")
    }

    override fun onTrackException(player: AudioPlayer?, track: AudioTrack?, exception: FriendlyException?) {
        exception?.printStackTrace()
        println("track exception")
    }

    override fun onTrackStart(player: AudioPlayer?, track: AudioTrack) {
        println("track started")
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack, endReason: AudioTrackEndReason) {
        println("track ended eventStartNext:" + endReason.mayStartNext)
        if (endReason.mayStartNext) nextTrack(track)
    }

    fun clear() {
        tracks.clear()
    }

    fun getPosition(audioTrack: AudioTrack): Int =
        if (iPlayer.playingTrack == audioTrack) {
            0
        } else {
            tracks.toList().indexOf(audioTrack) + 1
        }

    fun stop() {
        iPlayer.stopTrack()
    }

    fun skip(amount: Int) {
        var nextTrack: AudioTrack? = null
        for (i in 0 until amount) {
            nextTrack = tracks.poll()
        }
        if (nextTrack == null) {
            stop()
        } else {
            iPlayer.stopTrack()
            iPlayer.playTrack(nextTrack)
        }
    }

    fun setPaused(paused: Boolean) {
        iPlayer.isPaused = paused
    }

    fun removeAt(indexes: IntArray): Map<Int, AudioTrack> {
        val newQueue = LinkedList<AudioTrack>()
        val removed = HashMap<Int, AudioTrack>()

        tracks.forEachIndexed { index, track ->
            if (!indexes.contains(index)) {
                newQueue.add(track)
            } else {
                removed[index] = track
            }
        }
        tracks = newQueue
        return removed
    }
}