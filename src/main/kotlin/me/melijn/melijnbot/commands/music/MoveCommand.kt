package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.utils.MarkdownSanitizer

class MoveCommand : AbstractCommand("command.move") {

    init {
        id = 155
        name = "move"
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: ICommandContext) {
        val player = context.getGuildMusicPlayer()
        val trackManager = player.guildTrackManager
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val index1 = (getIntegerFromArgNMessage(context, 0, 1, trackManager.trackSize()) ?: return) - 1
        val index2 = (getIntegerFromArgNMessage(context, 1, 1, trackManager.trackSize()) ?: return) - 1

        if (index1 == index2) {
            val msg = context.getTranslation("$root.sameindex")
            sendRsp(context, msg)
            return
        }

        val trackList = trackManager.tracks
        val track = trackList.removeAt(index1)
        trackList.add(index2, track)

        val msg = context.getTranslation("$root.moved")
            .withVariable("pos1", "${index1 + 1}")
            .withVariable("pos2", "${index2 + 1}")
            .withVariable("track", MarkdownSanitizer.escape(track.info.title))

        sendRsp(context, msg)
    }
}