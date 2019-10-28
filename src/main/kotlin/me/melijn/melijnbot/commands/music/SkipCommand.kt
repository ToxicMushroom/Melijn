package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getIntegerFromArgN
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg

class SkipCommand : AbstractCommand("command.skip") {

    init {
        id = 83
        name = "skip"
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val amount = getIntegerFromArgN(context, 0, 1) ?: 1
        val trackManager = context.getGuildMusicPlayer().guildTrackManager
        val cTrack: AudioTrack? = trackManager.iPlayer.playingTrack
        if (cTrack == null) {
            val noSongPlaying = i18n.getTranslation(context, "message.music.notracks")
            sendMsg(context, noSongPlaying)
            return
        }
        val part1 = if (amount > 1) {
            i18n.getTranslation(context, "$root.skips")
                .replace("%amount%", amount.toString())
        } else {
            i18n.getTranslation(context, "$root.skip")
                .replace("%track%", cTrack.info.title)
        }
        context.getGuildMusicPlayer().guildTrackManager.skip(amount)
        val nTrack: AudioTrack? = trackManager.iPlayer.playingTrack
        val part2 = if (nTrack == null) {
            i18n.getTranslation(context, "$root.nonext")
        } else {
            i18n.getTranslation(context, "$root.next")
                .replace("%track%", nTrack.info.title)
        }

        val title = i18n.getTranslation(context, "$root.title")
            .replace(PLACEHOLDER_USER, context.getAuthor().asTag)
        val eb = Embedder(context)
        eb.setTitle(title)
        eb.setDescription(part1 + part2)
        sendEmbed(context, eb.build())
    }
}