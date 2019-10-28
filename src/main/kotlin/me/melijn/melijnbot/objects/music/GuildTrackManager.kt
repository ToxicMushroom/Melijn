package me.melijn.melijnbot.objects.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import lavalink.client.player.IPlayer
import lavalink.client.player.event.AudioEventAdapterWrapped
import java.util.*


class GuildTrackManager(
    val iPlayer: IPlayer
) : AudioEventAdapterWrapped() {

    var tracks: Queue<AudioTrack> = LinkedList()
    fun trackSize() = tracks.size


    fun nextTrack(lastTrack: AudioTrack) {
        if (tracks.isEmpty()) {
            iPlayer.stopTrack()
            return
        }

        val track: AudioTrack = tracks.poll()
        if (track == lastTrack) iPlayer.playTrack(track.makeClone()) else iPlayer.playTrack(track)
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

    override fun onTrackStart(player: AudioPlayer?, track: AudioTrack) {
        println("track started")
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack, endReason: AudioTrackEndReason) {
        println("track ended")
        nextTrack(track)
//        val guildId = iPlayer.link.guildIdLong
//        if (melijn.getVariables().looped.contains(guildId)) {
//            melijn.getLava().getAudioLoader().loadSimpleTrack(musicPlayer, track.info.uri)
//        } else if (melijn.getVariables().loopedQueues.contains(guildId)) {
//            if (endReason.mayStartNext) nextTrack(track)
//            melijn.getLava().getAudioLoader().loadSimpleTrack(musicPlayer, track.info.uri)
//        } else {
//            if (endReason.mayStartNext) nextTrack(track)
//        }
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
}