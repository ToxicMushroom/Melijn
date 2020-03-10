package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.utils.getDurationString
import me.melijn.melijnbot.objects.utils.listeningMembers
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg
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
        val guildMusicPlayer = context.guildMusicPlayer.guildTrackManager
        val vc = context.lavaManager.getConnectedChannel(context.guild)
        if (vc == null) {
            sendMsg(context, "wtf")
            return
        }
        val listening = listeningMembers(vc)

        guildMusicPlayer.votes++
        val requiredVotes = (floor(listening / 2.0) + 1).toInt()
        if (guildMusicPlayer.votes == requiredVotes) {
            doSkip(context, guildMusicPlayer.votes)
        } else {
            val eb = Embedder(context)
            val title = context.getTranslation("$root.progress.title")
                .replace("%votesRequired%", "$requiredVotes")
                .replace("%votes%", "${guildMusicPlayer.votes}")
            eb.setTitle(title)

            val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer
            val cTrack = iPlayer.playingTrack ?: return
            val desc = context.getTranslation("$root.playing")
                .replace("%track%", cTrack.info.title)
                .replace("%url%", cTrack.info.uri)
                .replace("%position%", getDurationString(iPlayer.trackPosition))
                .replace("%duration%", getDurationString(cTrack.duration))
            eb.setDescription(desc)
            sendEmbed(context, eb.build())

        }
    }

    suspend fun doSkip(context: CommandContext, votes: Int) {
        val trackManager = context.guildMusicPlayer.guildTrackManager
        val cTrack = trackManager.iPlayer.playingTrack ?: return
        val part1 =
            context.getTranslation("$root.skip")
                .replace("%track%", cTrack.info.title)
                .replace("%url%", cTrack.info.uri)
                .replace("%position%", getDurationString(trackManager.iPlayer.trackPosition))
                .replace("%duration%", getDurationString(cTrack.duration))

        trackManager.skip(1)
        val nTrack: AudioTrack? = trackManager.iPlayer.playingTrack

        val part2 = if (nTrack == null) {
            context.getTranslation("$root.nonext")
        } else {
            context.getTranslation("$root.next")
                .replace("%track%", nTrack.info.title)
                .replace("%url%", nTrack.info.uri)
                .replace("%duration%", getDurationString(nTrack.duration))
        }

        val s = if (votes > 1) "s" else ""
        val title = context.getTranslation("$root.title$s")
            .replace("%votes%", "$votes")

        val eb = Embedder(context)
        eb.setTitle(title)
        eb.setDescription(part1 + part2)
        sendEmbed(context, eb.build())
    }
}