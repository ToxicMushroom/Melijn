package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getDurationString
import me.melijn.melijnbot.objects.utils.getIntegerFromArgN
import me.melijn.melijnbot.objects.utils.sendEmbed

class SkipCommand : AbstractCommand("command.skip") {

    init {
        id = 83
        name = "skip"
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val amount = getIntegerFromArgN(context, 0, 1) ?: 1
        val trackManager = context.guildMusicPlayer.guildTrackManager
        val cTrack = trackManager.iPlayer.playingTrack ?: return
        val part1 = if (amount > 1) {
            i18n.getTranslation(context, "$root.skips")
                .replace("%amount%", amount.toString())
        } else {
            i18n.getTranslation(context, "$root.skip")
        }.replace("%track%", cTrack.info.title)
            .replace("%url%", cTrack.info.uri)
            .replace("%position%", getDurationString(trackManager.iPlayer.trackPosition))
            .replace("%duration%", getDurationString(cTrack.duration))
        context.guildMusicPlayer.guildTrackManager.skip(amount)
        val nTrack: AudioTrack? = trackManager.iPlayer.playingTrack
        val part2 = if (nTrack == null) {
            i18n.getTranslation(context, "$root.nonext")
        } else {
            i18n.getTranslation(context, "$root.next")
                .replace("%track%", nTrack.info.title)
                .replace("%url%", nTrack.info.uri)
                .replace("%duration%", getDurationString(nTrack.duration))
        }

        val title = i18n.getTranslation(context, "$root.title")
            .replace(PLACEHOLDER_USER, context.author.asTag)
        val eb = Embedder(context)
        eb.setTitle(title)
        eb.setDescription(part1 + part2)
        sendEmbed(context, eb.build())
    }
}