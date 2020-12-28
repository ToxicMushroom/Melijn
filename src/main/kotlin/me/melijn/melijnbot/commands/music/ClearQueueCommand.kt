package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

class ClearQueueCommand : AbstractCommand("command.clearqueue") {

    init {
        id = 234
        name = "clearQueue"
        aliases = arrayOf("cq")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: ICommandContext) {
        val trackManager = context.getGuildMusicPlayer().guildTrackManager
        val tracks = trackManager.trackSize()
        trackManager.clear()

        val msg = context.getTranslation("$root.cleared")
            .withVariable("count", tracks)
        sendRsp(context, msg)
    }
}