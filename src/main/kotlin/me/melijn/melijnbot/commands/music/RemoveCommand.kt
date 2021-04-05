package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.StringUtils
import me.melijn.melijnbot.internals.utils.getIntegersFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable

class RemoveCommand : AbstractCommand("command.remove") {

    init {
        id = 235
        name = "remove"
        aliases = arrayOf("deleteTrack", "removeTrack")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val trackManager = context.getGuildMusicPlayer().guildTrackManager
        val indexes = getIntegersFromArgsNMessage(context, 0, 1, trackManager.trackSize()) ?: return
        val removed = trackManager.removeAt(indexes)

        var msg = context.getTranslation("$root.removed")
            .withVariable("count", removed.size.toString())
        for ((index, track) in removed) {
            msg += "\n[#${index + 1}](${track.info.uri}) - ${track.info.title}"
        }
        val queueParts = StringUtils.splitMessage(msg)
        val eb = Embedder(context)
        for (queue in queueParts) {
            eb.setDescription(queue)
            sendEmbedRsp(context, eb.build())
        }
    }
}

