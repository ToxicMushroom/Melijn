package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import kotlin.math.floor


class VoteSkipCommand : AbstractCommand("command.voteskip") {

    init {
        id = 149
        name = "voteSkip"
        aliases = arrayOf("vs")
        runConditions = arrayOf(RunCondition.VC_BOT_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val guildMusicPlayer = context.getGuildMusicPlayer().guildTrackManager
        val vc = context.lavaManager.getConnectedChannel(context.guild)
        if (vc == null) {
            sendRsp(context, "wtf")
            return
        }
        val listening = listeningMembers(vc)

        guildMusicPlayer.votedUsers.addIfNotPresent(context.authorId)
        val requiredVotes = (floor(listening / 2.0) + 1).toInt()
        if (guildMusicPlayer.votedUsers.size == requiredVotes) {
            doSkip(context, guildMusicPlayer.votedUsers.size)
        } else {

            val title = context.getTranslation("$root.progress.title")
                .withVariable("votesRequired", "$requiredVotes")
                .withVariable("votes", "${guildMusicPlayer.votedUsers.size}")


            val iPlayer = context.getGuildMusicPlayer().guildTrackManager.iPlayer
            val cTrack = iPlayer.playingTrack ?: return
            val desc = context.getTranslation("$root.playing")
                .withSafeVariable("track", cTrack.info.title)
                .withVariable("url", cTrack.info.uri)
                .withVariable("position", getDurationString(iPlayer.trackPosition))
                .withVariable("duration", getDurationString(cTrack.duration))

            val eb = Embedder(context)
                .setTitle(title)
                .setDescription(desc)

            sendEmbedRsp(context, eb.build())
        }
    }

    private suspend fun doSkip(context: CommandContext, votes: Int) {
        val trackManager = context.getGuildMusicPlayer().guildTrackManager
        val cTrack = trackManager.iPlayer.playingTrack ?: return
        val part1 =
            context.getTranslation("$root.skip")
                .withVariable("track", cTrack.info.title)
                .withVariable("url", cTrack.info.uri)
                .withVariable("position", getDurationString(trackManager.iPlayer.trackPosition))
                .withVariable("duration", getDurationString(cTrack.duration))

        trackManager.skip(1)
        val nTrack: AudioTrack? = trackManager.iPlayer.playingTrack

        val part2 = if (nTrack == null) {
            context.getTranslation("$root.nonext")
        } else {
            context.getTranslation("$root.next")
                .withVariable("track", nTrack.info.title)
                .withVariable("url", nTrack.info.uri)
                .withVariable("duration", getDurationString(nTrack.duration))
        }

        val s = if (votes > 1) "s" else ""
        val title = context.getTranslation("$root.title$s")
            .withVariable("votes", "$votes")

        val eb = Embedder(context)
            .setTitle(title)
            .setDescription(part1 + part2)
        sendEmbedRsp(context, eb.build())
    }
}