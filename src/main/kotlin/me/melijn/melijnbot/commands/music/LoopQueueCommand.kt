package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendMsg

class LoopQueueCommand : AbstractCommand("command.loopqueue") {

    init {
        id = 91
        name = "loopQueue"
        aliases = arrayOf("repeatQueue", "queueRepeat", "loopQueue", "queueLoop")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val trackManager = context.getGuildMusicPlayer().guildTrackManager
        trackManager.loopedQueue = !trackManager.loopedQueue

        val msg = i18n.getTranslation(context, "$root." + if (trackManager.loopedQueue) "looped" else "unlooped")
        sendMsg(context, msg)
    }
}