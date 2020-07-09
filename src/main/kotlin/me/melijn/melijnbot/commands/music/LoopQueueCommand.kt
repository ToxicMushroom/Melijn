package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.message.sendRsp

class LoopQueueCommand : AbstractCommand("command.loopqueue") {

    init {
        id = 91
        name = "loopQueue"
        aliases = arrayOf("repeatQueue", "queueRepeat", "loopQueue", "queueLoop")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL, RunCondition.VOTED)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val trackManager = context.guildMusicPlayer.guildTrackManager
        trackManager.loopedQueue = !trackManager.loopedQueue

        val extra = if (trackManager.loopedQueue) {
            "looped"
        } else {
            "unlooped"
        }

        val msg = context.getTranslation("$root.$extra")
        sendRsp(context, msg)
    }
}