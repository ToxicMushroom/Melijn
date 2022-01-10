package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp

enum class SkipType {
    HARD, SOFT
}

class SkipCommand : AbstractCommand("command.skip") {

    init {
        id = 83
        name = "skip"
        aliases = arrayOf("s", "ss", "sskip", "softSkip")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: ICommandContext) {
        val softArg = context.optional(0, "hard") { context.args.getOrNull(it) } ?: "hard"
        val isSoft = setOf("soft", "-s").contains(softArg) ||
            context.commandParts[1].lowercase() == "sskip" ||
            context.commandParts[1].lowercase() == "ss" ||
            context.commandParts[1].lowercase() == "softskip"

        val skipType = if (isSoft) SkipType.SOFT else SkipType.HARD
        val amount = getIntegerFromArgN(context, 0, 1) ?: getIntegerFromArgN(context, 1, 1) ?: 1
        val trackManager = context.getGuildMusicPlayer().guildTrackManager
        val cTrack = trackManager.iPlayer.playingTrack ?: return
        val trackPos = try {
            trackManager.iPlayer.trackPosition
        } catch (e: Exception) {
            return
        }
        val part1 = if (amount > 1) {

            context.getTranslation("$root.skips.${skipType.toLC()}")
                .withVariable("amount", amount.toString())
        } else {
            context.getTranslation("$root.skip.${skipType.toLC()}")
        }
            .withSafeVariable("track", cTrack.info.title)
            .withVariable("url", cTrack.info.uri)
            .withVariable("position", getDurationString(trackPos))
            .withVariable("duration", getDurationString(cTrack.duration))

        context.getGuildMusicPlayer().guildTrackManager.skip(amount, skipType)
        val nTrack: AudioTrack? = trackManager.iPlayer.playingTrack

        val part2 = if (nTrack == null) {
            context.getTranslation("$root.nonext")
        } else {
            context.getTranslation("$root.next")
                .withSafeVariable("track", nTrack.info.title)
                .withVariable("url", nTrack.info.uri)
                .withVariable("duration", getDurationString(nTrack.duration))
        }

        val title = context.getTranslation("$root.title")
            .withVariable(PLACEHOLDER_USER, context.author.asTag)

        val eb = Embedder(context)
            .setTitle(title)
            .setDescription(part1 + part2)
        sendEmbedRsp(context, eb.build())
    }
}